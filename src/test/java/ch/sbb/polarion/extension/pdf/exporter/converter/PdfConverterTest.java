package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf.exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.ExportMetaInfoCallback;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.DocumentDataHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.tracker.internal.model.LinkRoleOpt;
import com.polarion.alm.tracker.internal.model.TypeOpt;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.spi.EnumOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfConverterTest {
    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;
    @Mock
    private IModule module;
    @Mock
    private DocumentDataHelper documentDataHelper;
    @Mock
    private CssSettings cssSettings;
    @Mock
    private HeaderFooterSettings headerFooterSettings;
    @Mock
    private PlaceholderProcessor placeholderProcessor;
    @Mock
    private VelocityEvaluator velocityEvaluator;
    @Mock
    private CoverPageProcessor coverPageProcessor;
    @Mock
    private WeasyPrintServiceConnector weasyPrintServiceConnector;
    @Mock
    private HtmlProcessor htmlProcessor;
    @Mock
    private PdfTemplateProcessor pdfTemplateProcessor;

    @Test
    void shouldConvertToPdfInSimplestWorkflow() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId("test project")
                .documentType(DocumentType.DOCUMENT)
                .build();
        ITrackerProject project = mock(ITrackerProject.class);
        lenient().when(pdfExporterPolarionService.getTrackerProject("test project")).thenReturn(project);
        PdfConverter pdfConverter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, documentDataHelper, placeholderProcessor, velocityEvaluator, coverPageProcessor, weasyPrintServiceConnector, htmlProcessor, pdfTemplateProcessor);
        CssModel cssModel = CssModel.builder().css("test css").build();
        when(cssSettings.load("test project", SettingId.fromName("Default"))).thenReturn(cssModel);
        DocumentData documentData = DocumentData.builder()
                .documentTitle("testDocument")
                .documentContent("test document content")
                .build();
        when(documentDataHelper.getLiveDocument(project, exportParams)).thenReturn(documentData);
        when(headerFooterSettings.load("test project", SettingId.fromName("Default"))).thenReturn(HeaderFooterModel.builder().build());
        when(placeholderProcessor.replacePlaceholders(documentData, exportParams, "test css")).thenReturn("css content");
        when(placeholderProcessor.replacePlaceholders(eq(documentData), eq(exportParams), anyList())).thenReturn(List.of("hl", "hc", "hr", "fl", "fc", "fr"));
        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        when(pdfTemplateProcessor.processUsing(eq(exportParams), eq("testDocument"), eq("css content"), anyString())).thenReturn("test html content");
        when(weasyPrintServiceConnector.convertToPdf("test html content", new WeasyPrintOptions())).thenReturn("test document content".getBytes());
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        // Act
        byte[] result = pdfConverter.convertToPdf(exportParams, null);

        // Assert
        assertThat(result).isEqualTo("test document content".getBytes());
    }

    @Test
    void shouldGetAndReplaceCss() {
        DocumentData documentData = DocumentData.builder()
                .projectName("testProjectName")
                .lastRevision("testLastRevision")
                .documentTitle("testDocumentTitle")
                .document(module)
                .build();

        ExportParams exportParams = ExportParams.builder()
                .css("testCssSetting")
                .projectId("testProjectId")
                .revision("testRevision")
                .numberedListStyles("testNumberedListStyles")
                .build();

        CssModel cssModel = CssModel.builder().css("my test css: {{ DOCUMENT_TITLE }} {{DOCUMENT_REVISION}} {{ REVISION }} {{ PRODUCT_NAME }} {{ PRODUCT_VERSION }} {{customField}}").build();
        when(cssSettings.load("testProjectId", SettingId.fromName("testCssSetting"))).thenReturn(cssModel);
        PlaceholderProcessor processor = new PlaceholderProcessor(pdfExporterPolarionService, documentDataHelper);
        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        PdfConverter pdfConverter = new PdfConverter(null, null, cssSettings, null, processor, velocityEvaluator, null, null, htmlProcessor, pdfTemplateProcessor);

        when(documentDataHelper.getDocumentStatus("testRevision", documentData)).thenReturn("testStatus");
        when(pdfExporterPolarionService.getPolarionProductName()).thenReturn("testProductName");
        when(pdfExporterPolarionService.getPolarionVersion()).thenReturn("testVersion");
        lenient().when(module.getCustomField("customField")).thenReturn("customValue");
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        String cssContent = pdfConverter.getCssContent(documentData, exportParams);

        assertThat(cssContent).startsWith("my test css: testDocumentTitle testStatus testRevision testProductName testVersion customValue");
    }

    @ParameterizedTest
    @MethodSource("paramsForHeaderFooterContent")
    @SuppressWarnings("unchecked")
    void shouldGetAndProcessHeaderFooterContent(String settingName, String settingArgument) {
        // Arrange
        DocumentData documentData = DocumentData.builder()
                .build();
        ExportParams exportParams = ExportParams.builder()
                .headerFooter(settingName)
                .projectId("testProjectId")
                .build();

        HeaderFooterModel headerFooterModel = HeaderFooterModel.builder()
                .headerLeft("-header-left-")
                .headerCenter("-header-center-")
                .headerRight("-header-right-")
                .footerLeft("-footer-left-")
                .footerCenter("-footer-center-")
                .footerRight("-footer-right-").build();
        when(headerFooterSettings.load("testProjectId", SettingId.fromName(settingArgument))).thenReturn(headerFooterModel);

        when(placeholderProcessor.replacePlaceholders(eq(documentData), eq(exportParams), any(List.class))).thenReturn(List.of(
                "-xheader-left-",
                "-xheader-center-",
                "-xheader-right-",
                "-xfooter-left-",
                "-xfooter-center-",
                "-xfooter-right-"));

        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        // Act
        PdfConverter pdfConverter = new PdfConverter(null, headerFooterSettings, null, null, placeholderProcessor, velocityEvaluator, null, null, htmlProcessor, null);
        String headerFooterContent = pdfConverter.getHeaderFooterContent(documentData, exportParams);

        // Assert
        assertThat(TestStringUtils.removeNonsensicalSymbols(headerFooterContent)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <div class='header top-center header-footer-wrapper'>
                    <div class='header-footer-left'>
                        -xheader-left-
                    </div>
                    <div class='header-footer-center'>
                        -xheader-center-
                    </div>
                    <div class='header-footer-right'>
                        -xheader-right-
                    </div>
                </div>
                <div class='footer bottom-center header-footer-wrapper'>
                    <div class='header-footer-left'>
                        -xfooter-left-
                    </div>
                    <div class='header-footer-center'>
                        -xfooter-center-
                    </div>
                    <div class='header-footer-right'>
                        -xfooter-right-
                    </div>
                </div>""".indent(0).trim()));

        ArgumentCaptor<List<String>> captorHeaderFooters = ArgumentCaptor.forClass(List.class);
        verify(placeholderProcessor).replacePlaceholders(eq(documentData), eq(exportParams), captorHeaderFooters.capture());
        assertThat(captorHeaderFooters.getValue()).containsExactly(
                "-header-left-",
                "-header-center-",
                "-header-right-",
                "-footer-left-",
                "-footer-center-",
                "-footer-right-");
    }

    private static Stream<Arguments> paramsForHeaderFooterContent() {
        return Stream.of(
                Arguments.of("testHeaderFooterSetting", "testHeaderFooterSetting"),
                Arguments.of(null, "Default")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPostProcessDocumentContent() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .linkedWorkitemRoles(List.of("role1", "role2"))
                .build();
        ITrackerProject project = mock(ITrackerProject.class);
        IEnumeration<ILinkRoleOpt> roleEnum = mock(IEnumeration.class);
        Properties props1 = new Properties();
        props1.put("oppositeName", "testRole1OppositeName");
        ILinkRoleOpt option1 = new LinkRoleOpt(new EnumOption("roleLink", "role1", "role1", 1, false, props1));
        Properties props2 = new Properties();
        props2.put("oppositeName", "testRole2OppositeName");
        ILinkRoleOpt option2 = new LinkRoleOpt(new EnumOption("roleLink", "role2", "role2", 1, false, props2));
        when(roleEnum.getAvailableOptions("wiType")).thenReturn(List.of(option1, option2));
        IEnumeration<ITypeOpt> typeEnum = mock(IEnumeration.class);
        TypeOpt typeOption = new TypeOpt(new EnumOption("wiType", "wiType", "WIType", 1, false));
        when(typeEnum.getAllOptions()).thenReturn(List.of(typeOption));
        when(project.getWorkItemTypeEnum()).thenReturn(typeEnum);
        when(project.getWorkItemLinkRoleEnum()).thenReturn(roleEnum);
        when(htmlProcessor.processHtmlForPDF(anyString(), eq(exportParams), any(List.class))).thenReturn("result string");

        // Act
        PdfConverter pdfConverter = new PdfConverter(null, null, null, null, null, null, null, null, htmlProcessor, null);
        String resultContent = pdfConverter.postProcessDocumentContent(exportParams, project, "test content");

        // Assert
        assertThat(resultContent).isEqualTo("result string");
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(htmlProcessor).processHtmlForPDF(eq("test content"), eq(exportParams), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue()).containsExactly("role1", "testRole1OppositeName", "role2", "testRole2OppositeName");
    }

    @ParameterizedTest
    @MethodSource("paramsForGeneratePdf")
    void shouldGeneratePdf(String internalContent, String coverPage, ExportMetaInfoCallback metaInfoCallback, boolean useCoverPageProcessor) {
        // Arrange
        DocumentData documentData = DocumentData.builder()
                .build();
        ExportParams exportParams = ExportParams.builder()
                .internalContent(internalContent)
                .coverPage(coverPage)
                .build();
        PdfGenerationLog pdfGenerationLog = new PdfGenerationLog();
        if (useCoverPageProcessor) {
            when(coverPageProcessor.generatePdfWithTitle(documentData, exportParams, "test html content", pdfGenerationLog)).thenReturn("pdf result".getBytes());
        } else {
            when(weasyPrintServiceConnector.convertToPdf("test html content", new WeasyPrintOptions())).thenReturn("pdf result".getBytes());
        }

        // Act
        PdfConverter pdfConverter = new PdfConverter(null, null, null, null, null, null, coverPageProcessor, weasyPrintServiceConnector, null, null);
        byte[] result = pdfConverter.generatePdf(documentData, exportParams, metaInfoCallback, "test html content", pdfGenerationLog);

        // Assert
        assertThat(result).isEqualTo("pdf result".getBytes());
        if (useCoverPageProcessor) {
            verify(coverPageProcessor).generatePdfWithTitle(documentData, exportParams, "test html content", pdfGenerationLog);
        } else {
            verify(weasyPrintServiceConnector).convertToPdf("test html content", new WeasyPrintOptions());
        }
    }

    private static Stream<Arguments> paramsForGeneratePdf() {
        return Stream.of(
                Arguments.of(null, "test cover page", null, true),
                Arguments.of(null, "test cover page", new ExportMetaInfoCallback(), false),
                Arguments.of("internal content", "test cover page", null, false),
                Arguments.of("internal content", "test cover page", new ExportMetaInfoCallback(), false),
                Arguments.of(null, null, null, false)
        );
    }
}