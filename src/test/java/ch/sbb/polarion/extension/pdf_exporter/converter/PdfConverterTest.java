package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.ExportMetaInfoCallback;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.tracker.internal.model.LinkRoleOpt;
import com.polarion.alm.tracker.internal.model.TypeOpt;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.spi.EnumOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class PdfConverterTest {
    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;
    @Mock
    private IModule module;
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

    private MockedStatic<DocumentDataFactory> documentDataFactoryMockedStatic;

    @BeforeEach
    void setUp() {
        documentDataFactoryMockedStatic = mockStatic(DocumentDataFactory.class);
    }

    @AfterEach
    void tearDown() {
        documentDataFactoryMockedStatic.close();
    }

    @Test
    void shouldConvertToPdfInSimplestWorkflow() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId("testProjectId")
                .documentType(DocumentType.LIVE_DOC)
                .build();

        ITrackerProject project = mock(ITrackerProject.class);
        lenient().when(pdfExporterPolarionService.getTrackerProject("testProjectId")).thenReturn(project);
        PdfConverter pdfConverter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, placeholderProcessor, velocityEvaluator, coverPageProcessor, weasyPrintServiceConnector, htmlProcessor, pdfTemplateProcessor);
        CssModel cssModel = CssModel.builder().css("test css").build();
        when(cssSettings.load("testProjectId", SettingId.fromName("Default"))).thenReturn(cssModel);
        when(cssSettings.defaultValues()).thenReturn(CssModel.builder().build());
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testDocument")
                .content("test document content")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(exportParams), anyBoolean())).thenReturn(documentData);
        when(headerFooterSettings.load("testProjectId", SettingId.fromName("Default"))).thenReturn(HeaderFooterModel.builder().useCustomValues(true).build());
        when(placeholderProcessor.replacePlaceholders(documentData, exportParams, "test css")).thenReturn("css content");
        when(placeholderProcessor.replacePlaceholders(eq(documentData), eq(exportParams), anyList())).thenReturn(List.of("hl", "hc", "hr", "fl", "fc", "fr"));
        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        when(pdfTemplateProcessor.processUsing(eq(exportParams), eq("testDocument"), eq("css content"), anyString(), anyString())).thenReturn("test html content");
        when(weasyPrintServiceConnector.convertToPdf(eq("test html content"), any(WeasyPrintOptions.class))).thenReturn("test document content".getBytes());
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        // Act
        byte[] result = pdfConverter.convertToPdf(exportParams, null);

        // Assert
        assertThat(result).isEqualTo("test document content".getBytes());
    }

    @Test
    void shouldGetAndReplaceCss() {
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .lastRevision("testLastRevision")
                .revisionPlaceholder("testRevisionPlaceholder")
                .title("testDocumentTitle")
                .build();

        ExportParams exportParams = ExportParams.builder()
                .css("testCssSetting")
                .projectId("testProjectId")
                .revision("testRevision")
                .numberedListStyles("testNumberedListStyles")
                .build();

        CssModel cssModel = CssModel.builder().css("my test css: {{ DOCUMENT_TITLE }} {{DOCUMENT_REVISION}} {{ REVISION }} {{ PRODUCT_NAME }} {{ PRODUCT_VERSION }} {{customField}}").build();
        when(cssSettings.load("testProjectId", SettingId.fromName("testCssSetting"))).thenReturn(cssModel);
        when(cssSettings.defaultValues()).thenReturn(CssModel.builder().build());
        PlaceholderProcessor processor = new PlaceholderProcessor(pdfExporterPolarionService);
        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        PdfConverter pdfConverter = new PdfConverter(null, null, cssSettings, processor, velocityEvaluator, null, null, htmlProcessor, pdfTemplateProcessor);

        when(pdfExporterPolarionService.getPolarionProductName()).thenReturn("testProductName");
        when(pdfExporterPolarionService.getPolarionVersion()).thenReturn("testVersion");
        lenient().when(module.getCustomField("customField")).thenReturn("customValue");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        String cssContent = pdfConverter.getCssContent(documentData, exportParams);

        assertThat(cssContent).startsWith("my test css: testDocumentTitle testRevisionPlaceholder testRevision testProductName testVersion customValue");
    }

    @ParameterizedTest
    @MethodSource("paramsForHeaderFooterContent")
    @SuppressWarnings("unchecked")
    void shouldGetAndProcessHeaderFooterContent(String settingName, String settingArgument) {
        // Arrange
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testDocumentTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();
        ExportParams exportParams = ExportParams.builder()
                .headerFooter(settingName)
                .projectId("testProjectId")
                .build();

        HeaderFooterModel headerFooterModel = HeaderFooterModel.builder()
                .useCustomValues(true)
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
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(a -> a.getArgument(0));

        // Act
        PdfConverter pdfConverter = new PdfConverter(null, headerFooterSettings, null, placeholderProcessor, velocityEvaluator, null, null, htmlProcessor, null);
        String headerFooterContent = pdfConverter.getHeaderFooterContent(documentData, exportParams);

        // Assert
        assertThat(TestStringUtils.removeNonsensicalSymbols(headerFooterContent)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <div class='header'>
                    <div class='top-left'>
                        -xheader-left-
                    </div>
                    <div class='top-center'>
                        -xheader-center-
                    </div>
                    <div class='top-right'>
                        -xheader-right-
                    </div>
                </div>
                <div class='footer'>
                    <div class='bottom-left'>
                        -xfooter-left-
                    </div>
                    <div class='bottom-center'>
                        -xfooter-center-
                    </div>
                    <div class='bottom-right'>
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
        PdfConverter pdfConverter = new PdfConverter(null, null, null, null, null, null, null, htmlProcessor, null);
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
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testDocumentTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();
        ExportParams exportParams = ExportParams.builder()
                .internalContent(internalContent)
                .coverPage(coverPage)
                .build();
        PdfGenerationLog pdfGenerationLog = new PdfGenerationLog();
        if (useCoverPageProcessor) {
            when(coverPageProcessor.generatePdfWithTitle(eq(documentData), eq(exportParams), eq("test html content"), any(WeasyPrintOptions.class), eq(pdfGenerationLog))).thenReturn("pdf result".getBytes());
        } else {
            when(weasyPrintServiceConnector.convertToPdf(eq("test html content"), any(WeasyPrintOptions.class))).thenReturn("pdf result".getBytes());
        }

        // Act
        PdfConverter pdfConverter = new PdfConverter(null, null, null, null, null, coverPageProcessor, weasyPrintServiceConnector, null, null);
        byte[] result = pdfConverter.generatePdf(documentData, exportParams, metaInfoCallback, "test html content", pdfGenerationLog);

        // Assert
        assertThat(result).isEqualTo("pdf result".getBytes());
        if (useCoverPageProcessor) {
            verify(coverPageProcessor).generatePdfWithTitle(eq(documentData), eq(exportParams), eq("test html content"), any(WeasyPrintOptions.class), eq(pdfGenerationLog));
        } else {
            verify(weasyPrintServiceConnector).convertToPdf(eq("test html content"), any(WeasyPrintOptions.class));
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
