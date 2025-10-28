package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import ch.sbb.polarion.extension.pdf_exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Base class for PDF converter integration tests.
 * Provides common setup, teardown, and utility methods for testing PDF conversion functionality.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BasePdfConverterTest extends BaseWeasyPrintTest {

    public static final String PAGE_BREAK = "<div style='break-after:page'></div>";

    @SuppressWarnings("rawtypes")
    protected MockedStatic<CompletableFuture> completableFutureMockedStatic;
    protected MockedStatic<DocumentDataFactory> documentDataFactoryMockedStatic;

    protected PdfExporterPolarionService pdfExporterPolarionService;
    protected HeaderFooterSettings headerFooterSettings;
    protected CssSettings cssSettings;
    protected LocalizationSettings localizationSettings;
    protected CoverPageSettings coverPageSettings;
    protected PlaceholderProcessor placeholderProcessor;
    protected VelocityEvaluator velocityEvaluator;
    protected HtmlProcessor htmlProcessor;
    protected PdfConverter converter;
    protected IModule module;

    @BeforeEach
    protected void setUp() {
        prepareBaseMocks();

        // Change behavior for CompletableFuture.supplyAsync() from async to sync
        // because we have static mocks which cannot be shared between threads
        completableFutureMockedStatic = mockStatic(CompletableFuture.class, invocation -> {
            if (invocation.getMethod().getName().equals("supplyAsync")) {
                Supplier<?> supplier = invocation.getArgument(0);
                return CompletableFuture.completedFuture(supplier.get());
            }
            return invocation.callRealMethod();
        });

        documentDataFactoryMockedStatic = mockStatic(DocumentDataFactory.class);

        setupConverter();
    }

    @AfterEach
    protected void tearDown() {
        if (completableFutureMockedStatic != null) {
            completableFutureMockedStatic.close();
        }
        if (documentDataFactoryMockedStatic != null) {
            documentDataFactoryMockedStatic.close();
        }
    }

    /**
     * Prepares base mocks that are common for all PDF converter tests.
     */
    @SneakyThrows
    protected void prepareBaseMocks() {
        ITrackerService trackerService = mock(ITrackerService.class);
        IProjectService projectService = mock(IProjectService.class);

        pdfExporterPolarionService = new PdfExporterPolarionService(
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

        setupBasicSettings();
        setupHelperComponents();
    }

    /**
     * Sets up basic settings (localization, header/footer, CSS, cover page).
     * Can be overridden in subclasses for custom behavior.
     */
    protected void setupBasicSettings() {
        localizationSettings = mock(LocalizationSettings.class);
        when(localizationSettings.load(any(), any())).thenReturn(new LocalizationModel(null, null, null));

        headerFooterSettings = mock(HeaderFooterSettings.class);
        setupHeaderFooterSettings();

        cssSettings = mock(CssSettings.class);
        when(cssSettings.defaultValues()).thenCallRealMethod();
        setupCssSettings();

        coverPageSettings = mock(CoverPageSettings.class);
        setupCoverPageSettings();
    }

    /**
     * Setup header/footer settings. Override in subclasses for custom headers/footers.
     */
    protected void setupHeaderFooterSettings() {
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("HL")
                .headerCenter("HC")
                .headerRight("HR")
                .footerLeft("FL")
                .footerCenter("FC")
                .footerRight("FR")
                .build());
    }

    /**
     * Setup CSS settings. Override in subclasses for custom CSS.
     */
    protected void setupCssSettings() {
        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(basicCss)
                .build());
    }

    /**
     * Setup cover page settings. Override in subclasses for custom cover pages.
     */
    protected void setupCoverPageSettings() {
        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<div>Cover Page Title</div>")
                .templateCss(basicCss)
                .build());
        lenient().when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Sets up helper components (placeholder processor, velocity evaluator, etc.).
     */
    protected void setupHelperComponents() {
        placeholderProcessor = new PlaceholderProcessor(pdfExporterPolarionService);

        velocityEvaluator = mock(VelocityEvaluator.class);
        when(velocityEvaluator.evaluateVelocityExpressions(any(), anyString())).thenAnswer(a -> a.getArguments()[1]);

        HtmlLinksHelper htmlLinksHelper = mock(HtmlLinksHelper.class);
        when(htmlLinksHelper.internalizeLinks(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FileResourceProvider fileResourceProvider = mock(FileResourceProvider.class);
        htmlProcessor = new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper);
    }

    /**
     * Sets up the PDF converter with all required dependencies.
     */
    protected void setupConverter() {
        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(
                placeholderProcessor,
                velocityEvaluator,
                getWeasyPrintServiceConnector(),
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
                getWeasyPrintServiceConnector(),
                htmlProcessor,
                new PdfTemplateProcessor()
        );
    }

    /**
     * Compares PDF content with reference images.
     * Writes report PDFs and images for debugging when tests fail.
     *
     * @return true if there were differences, false otherwise
     */
    @SneakyThrows
    protected boolean compareContentUsingReferenceImages(String testName, byte[] pdf) {
        writeReportPdf(testName, "generated", pdf);
        // NOTE: if something changes in the future and the images are no longer identical,
        // simply copy & replace the reference resource images with the new ones from the reports folder
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
        return hasDiff;
    }

}
