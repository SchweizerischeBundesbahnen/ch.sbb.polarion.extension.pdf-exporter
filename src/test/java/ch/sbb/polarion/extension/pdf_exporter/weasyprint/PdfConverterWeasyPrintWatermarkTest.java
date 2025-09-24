package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyBoolean;

@ExtendWith(MockitoExtension.class)
class PdfConverterWeasyPrintWatermarkTest extends BaseWeasyPrintTest {

    public static final String PAGE_BREAK = "<div style='break-after:page'></div>";
    @SuppressWarnings("rawtypes")
    private MockedStatic<CompletableFuture> completableFutureMockedStatic;

    private PdfConverter converter;
    private IModule module;

    private MockedStatic<DocumentDataFactory> documentDataFactoryMockedStatic;

    @BeforeEach
    void setUp() {
        prepareTestMocks();

        // we need to change behavior for CompletableFuture.supplyAsync() from async to sync
        // because we have static mocks which can not be shared between threads
        completableFutureMockedStatic = mockStatic(CompletableFuture.class, invocation -> {
            if (invocation.getMethod().getName().equals("supplyAsync")) {
                Supplier<?> supplier = invocation.getArgument(0);
                return CompletableFuture.completedFuture(supplier.get());
            }
            return invocation.callRealMethod();
        });

        documentDataFactoryMockedStatic = mockStatic(DocumentDataFactory.class);
    }

    @AfterEach
    void tearDown() {
        completableFutureMockedStatic.close();
        documentDataFactoryMockedStatic.close();
    }

    @SneakyThrows
    void prepareTestMocks() {

        ITrackerService trackerService = mock(ITrackerService.class);
        IProjectService projectService = mock(IProjectService.class);
        PdfExporterPolarionService pdfExporterPolarionService = new PdfExporterPolarionService(
                trackerService,
                projectService,
                mock(ISecurityService.class),
                mock(IPlatformService.class),
                mock(IRepositoryService.class),
                mock(ITestManagementService.class)
        );

        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(any(IProject.class))).thenReturn(trackerProject);
        lenient().when(trackerProject.isUnresolvable()).thenReturn(false);

        module = mock(IModule.class);
        lenient().when(module.getCustomField(anyString())).thenAnswer((Answer<String>) invocation ->
                "testFieldKey".equals(invocation.getArgument(0)) ? "testFieldValue" : null);

        LocalizationSettings localizationSettings = mock(LocalizationSettings.class);
        when(localizationSettings.load(any(), any())).thenReturn(new LocalizationModel(null, null, null));

        HeaderFooterSettings headerFooterSettings = mock(HeaderFooterSettings.class);
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("HL")
                .headerCenter("HC")
                .headerRight("HR")
                .footerLeft("FL")
                .footerCenter("FC {{ PAGE_NUMBER }}")
                .footerRight("FR")
                .build());

        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        CssSettings cssSettings = mock(CssSettings.class);
        when(cssSettings.defaultValues()).thenCallRealMethod();

        // Use default CSS which already includes watermark styles in @media print block
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(basicCss)
                .build());

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        lenient().when(weasyPrintServiceConnector.convertToPdf(anyString(), any())).thenAnswer((Answer<byte[]>) invocation -> exportToPdf(invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(weasyPrintServiceConnector.convertToPdf(anyString(), any(), any())).thenAnswer((Answer<byte[]>) invocation -> exportToPdf(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        PlaceholderProcessor placeholderProcessor = new PlaceholderProcessor(pdfExporterPolarionService);

        VelocityEvaluator velocityEvaluator = mock(VelocityEvaluator.class);
        when(velocityEvaluator.evaluateVelocityExpressions(any(), anyString())).thenAnswer(a -> a.getArguments()[1]);

        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<div>Cover Page Title</div>")
                .templateCss(basicCss)
                .build());
        lenient().when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HtmlLinksHelper htmlLinksHelper = mock(HtmlLinksHelper.class);
        when(htmlLinksHelper.internalizeLinks(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FileResourceProvider fileResourceProvider = mock(FileResourceProvider.class);
        HtmlProcessor htmlProcessor = new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper, pdfExporterPolarionService);
        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(
                placeholderProcessor,
                velocityEvaluator,
                weasyPrintServiceConnector,
                coverPageSettings,
                new PdfTemplateProcessor(),
                htmlProcessor
        );

        converter = new PdfConverter(
                pdfExporterPolarionService,
                headerFooterSettings,
                cssSettings,
                placeholderProcessor,
                velocityEvaluator,
                coverPageProcessor,
                weasyPrintServiceConnector,
                htmlProcessor,
                new PdfTemplateProcessor()
        );
    }

    @Test
    void testConverterWithWatermark() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Watermarked Document")
                .content("<h1>Confidential Document</h1><p>This document should have a watermark.</p><p>The watermark should say 'Confidential' and be visible across the page.</p>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterWithoutWatermark() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(false)  // Disable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Regular Document")
                .content("<h1>Regular Document</h1><p>This document should NOT have a watermark.</p>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterWithWatermarkMultiplePages() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Multi-page Watermarked Document")
                .content("""
                        <h1>Page 1</h1>
                        <p>First page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 2</h1>
                        <p>Second page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 3</h1>
                        <p>Third page content with watermark.</p>
                        """)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    @SneakyThrows
    void testConverterWithWatermarkAndCoverPage() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .coverPage("test")  // Enable cover page
                .build();

        Path tempFile = Files.createTempFile("attachment", ".pdf");
        Files.writeString(tempFile, "test attachment content");

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Document with Cover and Watermark")
                .content("""
                        <h1>Page 1</h1>
                        <p>First page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 2</h1>
                        <p>Second page content with watermark.</p>
                        """)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .attachmentFiles(List.of(tempFile))
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @SneakyThrows
    private void compareContentUsingReferenceImages(String testName, byte[] pdf) {
        writeReportPdf(testName, "generated", pdf);
        //NOTE: if something changes in the future and the images are no longer identical, simply copy&replace the reference resource images
        //with the new ones from the reports folder after test execution
        List<BufferedImage> resultImages = getAllPagesAsImagesAndLogAsReports(testName, pdf);
        boolean hasDiff = false;
        for (int i = 0; i < resultImages.size(); i++) {
            BufferedImage expectedImage = ImageIO.read(readPngResource(testName + PAGE_SUFFIX + i));
            BufferedImage resultImage = resultImages.get(i);
            List<Point> diffPoints = MediaUtils.diffImages(expectedImage, resultImage);
            if (!diffPoints.isEmpty()) {
                MediaUtils.fillImagePoints(resultImage, diffPoints, Color.BLUE.getRGB());
                writeReportImage(String.format("%s%s%d_diff", testName, PAGE_SUFFIX, i), resultImage);
                hasDiff = true;
            }
        }
        assertFalse(hasDiff);
    }
}
