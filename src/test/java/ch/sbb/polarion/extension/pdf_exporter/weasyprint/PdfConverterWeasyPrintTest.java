package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.WikiPageId;
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
import com.polarion.alm.tracker.model.IWikiPage;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfConverterWeasyPrintTest extends BaseWeasyPrintTest {

    @SuppressWarnings("rawtypes")
    private MockedStatic<CompletableFuture> completableFutureMockedStatic;

    private HeaderFooterSettings headerFooterSettings;
    private PdfConverter converter;
    private IModule module;

    private MockedStatic<DocumentDataFactory> documentDataFactoryMockedStatic;

    @BeforeEach
    protected void setUp() {
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
    protected void tearDown() {
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

        headerFooterSettings = mock(HeaderFooterSettings.class);
        //here we will test "testFieldKey" custom field & special "PAGE_NUMBER" placeholder substitution in the header
        when(headerFooterSettings.load(any(), any())).thenReturn(new HeaderFooterModel("HL", "HC  {{ testFieldKey }}", "HR", "FL", "FC", "FR_{{ PAGE_NUMBER }}"));

        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        CssSettings cssSettings = mock(CssSettings.class);
        when(cssSettings.defaultValues()).thenCallRealMethod();
        String defaultCss = cssSettings.defaultValues().getCss();
        //here we concatenate basic css with the default one in order to override font everywhere (also we have to cut some lines to achieve that)
        when(cssSettings.load(any(), any())).thenReturn(new CssModel(basicCss + defaultCss.replaceAll("@font-face[^}]+}", "").replaceAll("font-family:[^;]+;", "font-family: Custom Font;")));

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.convertToPdf(anyString(), any())).thenAnswer((Answer<byte[]>) invocation -> exportToPdf(invocation.getArgument(0), invocation.getArgument(1)));

        PlaceholderProcessor placeholderProcessor = new PlaceholderProcessor(pdfExporterPolarionService);

        VelocityEvaluator velocityEvaluator = mock(VelocityEvaluator.class);
        when(velocityEvaluator.evaluateVelocityExpressions(any(), anyString())).thenAnswer(a -> a.getArguments()[1]);

        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        //check "testFieldKey" custom field substitution in the title + use basic css here, otherwise fonts may differ on different OS
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(new CoverPageModel("<dev>TITLE {{ testFieldKey }}</div>", basicCss));
        lenient().when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(
                placeholderProcessor,
                velocityEvaluator,
                weasyPrintServiceConnector,
                coverPageSettings,
                new PdfTemplateProcessor()
        );

        HtmlLinksHelper htmlLinksHelper = mock(HtmlLinksHelper.class);
        when(htmlLinksHelper.internalizeLinks(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FileResourceProvider fileResourceProvider = mock(FileResourceProvider.class);

        converter = new PdfConverter(
                pdfExporterPolarionService,
                headerFooterSettings,
                cssSettings,
                placeholderProcessor,
                velocityEvaluator,
                coverPageProcessor,
                weasyPrintServiceConnector,
                new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper, pdfExporterPolarionService),
                new PdfTemplateProcessor()
        );
    }

    @Test
    void testConverterSimple() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc1 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .content("<div>TEST</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc1);

        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterComplexWithTitle() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .coverPage("test")
                .build();

        DocumentData<IModule> liveDoc2 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .content("<div>TEST page 1</div><!--PAGE_BREAK--><!--PORTRAIT_ABOVE--><div>TEST page 2</div><!--PAGE_BREAK--><!--LANDSCAPE_ABOVE--><div>TEST page 3</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc2);

        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterSpecialSymbols() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc3 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("specialSymbolsTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("specialSymbols"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc3);

        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterSvgImage() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc4 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("svgImageTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("svgImage"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc4);

        compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
    }

    @Test
    void testConverterWiki() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        //test wiki page export + {{ REVISION }} placeholder usage
        DocumentData<IWikiPage> wikiPage = DocumentData.creator(DocumentType.WIKI_PAGE, mock(IWikiPage.class))
                .id(new WikiPageId(new DocumentProject("testProjectId", "Test Project"), "wikiPageId"))
                .title("wikiPage")
                .content("<div>TEST</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(wikiPage);
        when(headerFooterSettings.load(any(), any())).thenReturn(new HeaderFooterModel("HL", "HC  {{ REVISION }}", "HR", "FL", "FC", "FR_{{ PAGE_NUMBER }}"));
        params.setDocumentType(DocumentType.WIKI_PAGE);
        params.setLocationPath("wikiFolder/wikiPage");

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
