package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverPageProcessorTest {
    @Mock
    private PlaceholderProcessor placeholderProcessor;
    @Mock
    private VelocityEvaluator velocityEvaluator;
    @Mock
    private WeasyPrintServiceConnector weasyPrintServiceConnector;
    @Mock
    private CoverPageSettings coverPageSettings;
    @Mock
    private PdfTemplateProcessor pdfTemplateProcessor;

    @InjectMocks
    private CoverPageProcessor coverPageProcessor;

    @Test
    @SneakyThrows
    void shouldInvokePdfGeneration() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId("testProjectId")
                .coverPage("test cover page")
                .build();
        CoverPageModel coverPageModel = CoverPageModel.builder()
                .templateHtml("test template html")
                .templateCss("test template css")
                .build();
        DocumentData<IModule> documentData = prepareMocks(coverPageModel, exportParams);
        when(weasyPrintServiceConnector.convertToPdf("result title html", new WeasyPrintOptions())).thenReturn(createEmptyPdf(2));
        when(weasyPrintServiceConnector.convertToPdf("test content", new WeasyPrintOptions())).thenReturn(createEmptyPdf(3));

        // Act
        byte[] result = coverPageProcessor.generatePdfWithTitle(documentData, exportParams, "test content", new PdfGenerationLog());

        // Assert
        try (PDDocument document = Loader.loadPDF(result)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }
    }

    @SneakyThrows
    private byte[] createEmptyPdf(int pageNumber) {
        ByteArrayOutputStream bos;
        try (PDDocument document = new PDDocument()) {
            IntStream.range(0, pageNumber)
                    .forEach(i -> {
                            PDPage pdPage = new PDPage();
                            document.addPage(pdPage);
                    });
            bos = new ByteArrayOutputStream();
            document.save(bos);
        }
        return bos.toByteArray();
    }

    @Test
    void shouldComposeTitleHtml() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId("testProjectId")
                .coverPage("test cover page")
                .build();
        CoverPageModel coverPageModel = CoverPageModel.builder()
                .templateHtml("test template html")
                .templateCss("test template css")
                .build();
        DocumentData<IModule> documentData = prepareMocks(coverPageModel, exportParams);

        // Act
        String result = coverPageProcessor.composeTitleHtml(documentData, exportParams);

        // Assert
        assertThat(result).isEqualTo("result title html");
    }

    private DocumentData<IModule> prepareMocks(CoverPageModel coverPageModel, ExportParams exportParams) {
        when(coverPageSettings.load("testProjectId", SettingId.fromName("test cover page"))).thenReturn(coverPageModel);
        DocumentData<IModule> documentData = DocumentData.<IModule>builder()
                .documentType(DocumentType.DOCUMENT)
                .documentObject(mock(IModule.class))
                .documentTitle("test document")
                .build();
        when(placeholderProcessor.replacePlaceholders(documentData, exportParams, "test template html")).thenReturn("replaced template html");
        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);
        when(coverPageSettings.processImagePlaceholders("test template css")).thenCallRealMethod();
        when(pdfTemplateProcessor.processUsing(exportParams, "test document", "test template css", "replaced template html")).thenReturn("result title html");
        return documentData;
    }
}