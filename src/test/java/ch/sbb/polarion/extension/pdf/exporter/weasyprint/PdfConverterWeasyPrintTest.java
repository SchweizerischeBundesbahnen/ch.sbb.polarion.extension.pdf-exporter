package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf.exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.LiveDocHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfConverterWeasyPrintTest extends BaseWeasyPrintTest {

    @Test
    @SneakyThrows
    void testConverter() {

        String testName = getCurrentMethodName();

        ITrackerService trackerService = mock(ITrackerService.class);
        IProjectService projectService = mock(IProjectService.class);
        PdfExporterPolarionService pdfExporterPolarionService = new PdfExporterPolarionService(trackerService, projectService,
                mock(ISecurityService.class), mock(IPlatformService.class), mock(IRepositoryService.class));

        IProject project = mock(IProject.class);
        when(projectService.getProject(anyString())).thenReturn(project);
        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(any(IProject.class))).thenReturn(trackerProject);
        lenient().when(trackerProject.isUnresolvable()).thenReturn(false);

        IModule module = mock(IModule.class);
        when(module.getCustomField(anyString())).thenAnswer((Answer<String>) invocation ->
                "testFieldKey".equals(invocation.getArgument(0)) ? "testFieldValue" : null);

        LiveDocHelper liveDocHelper = mock(LiveDocHelper.class);
        when(liveDocHelper.getLiveDocument(any(), any(), anyBoolean())).thenReturn(
                LiveDocHelper.DocumentData.builder().projectName("Test").document(module).documentTitle("testTitle").documentContent("<div>TEST</div>").lastRevision("42").build());

        LocalizationSettings localizationSettings = mock(LocalizationSettings.class);
        when(localizationSettings.load(any(), any())).thenReturn(new LocalizationModel(null, null, null));

        HeaderFooterSettings headerFooterSettings = mock(HeaderFooterSettings.class);
        //here we will test "testFieldKey" custom field & special "PAGE_NUMBER" placeholder substitution in the header
        when(headerFooterSettings.load(any(), any())).thenReturn(new HeaderFooterModel("HL", "HC  {{ testFieldKey }}", "HR", "FL", "FC", "FR {{ PAGE_NUMBER }}"));

        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        CssSettings cssSettings = mock(CssSettings.class);
        when(cssSettings.defaultValues()).thenCallRealMethod();
        String defaultCss = cssSettings.defaultValues().getCss();
        //here we concatenate basic css witch the default one in order to override font everywhere (also we have to cut some lines to achieve that)
        when(cssSettings.load(any(), any())).thenReturn(new CssModel(basicCss + defaultCss.replaceAll("font-family:[^;]+;", "")));

        WeasyPrintConverter weasyPrintConverter = mock(WeasyPrintConverter.class);
        when(weasyPrintConverter.convertToPdf(anyString(), any())).thenAnswer((Answer<byte[]>) invocation -> exportToPdf(invocation.getArgument(0), invocation.getArgument(1)));

        PlaceholderProcessor placeholderProcessor = new PlaceholderProcessor(pdfExporterPolarionService, liveDocHelper);

        VelocityEvaluator velocityEvaluator = mock(VelocityEvaluator.class);
        when(velocityEvaluator.evaluateVelocityExpressions(any(), anyString())).thenAnswer(a -> a.getArguments()[1]);

        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        //check "testFieldKey" custom field substitution in the title + use basic css here, otherwise fonts may differ on different OS
        when(coverPageSettings.load(any(), any())).thenReturn(new CoverPageModel("<dev>TITLE {{ testFieldKey }}</div>", basicCss));
        when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(placeholderProcessor, velocityEvaluator, weasyPrintConverter, coverPageSettings, new PdfTemplateProcessor());

        ExportParams params = ExportParams.builder().projectId("test").locationPath("testLocation").orientation(Orientation.PORTRAIT).paperSize(PaperSize.A4).build();
        PdfConverter converter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, liveDocHelper, placeholderProcessor, velocityEvaluator,
                coverPageProcessor, weasyPrintConverter, new HtmlProcessor(null, localizationSettings), new PdfTemplateProcessor());

        compareContentUsingReferenceImages(testName + "_simple", converter.convertToPdf(params, null));

        params.setCoverPage("test");
        when(liveDocHelper.getLiveDocument(any(), any(), anyBoolean())).thenReturn(
                LiveDocHelper.DocumentData.builder().projectName("Test").document(module).documentTitle("testTitle")
                        .documentContent("<div>TEST page 1</div><!--PAGE_BREAK--><!--PORTRAIT_ABOVE--><div>TEST page 2</div><!--PAGE_BREAK--><!--LANDSCAPE_ABOVE--><div>TEST page 3</div>")
                        .lastRevision("42")
                        .build());
        compareContentUsingReferenceImages(testName + "_complex_with_title", converter.convertToPdf(params, null));

        //test wiki page export + {{ REVISION }} placeholder usage
        when(liveDocHelper.getWikiDocument(any(), any())).thenReturn(
                LiveDocHelper.DocumentData.builder().projectName("Test").documentTitle("wikiPage").documentContent("<div>TEST</div>").lastRevision("42").build());
        when(headerFooterSettings.load(any(), any())).thenReturn(new HeaderFooterModel("HL", "HC  {{ REVISION }}", "HR", "FL", "FC", "FR {{ PAGE_NUMBER }}"));
        params.setCoverPage(null);
        params.setDocumentType(DocumentType.WIKI);
        params.setLocationPath("wikiFolder/wikiPage");
        compareContentUsingReferenceImages(testName + "_wiki", converter.convertToPdf(params, null));
    }

    @SneakyThrows
    private void compareContentUsingReferenceImages(String testName, byte[] pdf) {
        //NOTE: if something will be changed in the future and images stop being equal then just copy&replace reference resource images
        //with the new ones from the reports folder after test execution
        List<BufferedImage> resultImages = getAllPagesAsImagesAndLogAsReports(testName, pdf);
        for (int i = 0; i < resultImages.size(); i++) {
            BufferedImage expectedImage = ImageIO.read(readPngResource(testName + PAGE_SUFFIX + i));
            assertTrue(MediaUtils.compareImages(expectedImage, resultImages.get(i)));
        }
    }
}
