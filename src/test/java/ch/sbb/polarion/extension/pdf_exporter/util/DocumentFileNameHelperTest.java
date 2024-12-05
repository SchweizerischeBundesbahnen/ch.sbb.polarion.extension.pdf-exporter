package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentFileNameHelperTest {
    @Mock
    VelocityEvaluator velocityEvaluator;

    @InjectMocks
    DocumentFileNameHelper fileNameHelper;

    @Test
    void evaluateVelocity() {
        DocumentData<IModule> documentData = DocumentData.builder(DocumentType.LIVE_DOC, mock(IModule.class))
                .id("Test Id")
                .title("Test Title")
                .projectName("Test Project")
                .build();
        FileNameTemplateModel settingOneModel = FileNameTemplateModel.builder()
                .documentNameTemplate("$projectName $document.moduleFolder $document.moduleName")
                .build();

        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);

        String result = fileNameHelper.evaluateVelocity(documentData, settingOneModel.getDocumentNameTemplate());
        assertThat(result).contains("pdf");
    }

    @Test
    void getDocumentFileNameWithMissingProjectIdForLiveDocOrTestRun() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId(null)
                .documentType(DocumentType.LIVE_DOC)
                .build();
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileNameHelper.getDocumentFileName(exportParams));
        assertEquals("Project ID must be provided for LiveDoc or TestRun export", exception.getMessage());
    }

    @Test
    void getDocumentFileNameWithUnsupportedDocumentType() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .documentType(DocumentType.BASELINE_COLLECTION)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileNameHelper.getDocumentFileName(exportParams));
        assertEquals("Unsupported document type: BASELINE_COLLECTION", exception.getMessage());
    }


    @Test
    void getFileNameTemplate() {
        // Arrange
        FileNameTemplateModel mockModel = mock(FileNameTemplateModel.class);
        when(mockModel.getDocumentNameTemplate()).thenReturn("DocumentTemplate");
        when(mockModel.getReportNameTemplate()).thenReturn("ReportTemplate");
        when(mockModel.getTestRunNameTemplate()).thenReturn("TestRunTemplate");
        when(mockModel.getWikiNameTemplate()).thenReturn("WikiTemplate");

        // Act & Assert
        assertEquals("DocumentTemplate", fileNameHelper.getFileNameTemplate(DocumentType.LIVE_DOC, mockModel));
        assertEquals("ReportTemplate", fileNameHelper.getFileNameTemplate(DocumentType.LIVE_REPORT, mockModel));
        assertEquals("TestRunTemplate", fileNameHelper.getFileNameTemplate(DocumentType.TEST_RUN, mockModel));
        assertEquals("WikiTemplate", fileNameHelper.getFileNameTemplate(DocumentType.WIKI_PAGE, mockModel));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                fileNameHelper.getFileNameTemplate(DocumentType.BASELINE_COLLECTION, mockModel));
        assertEquals("Unsupported document type: BASELINE_COLLECTION", exception.getMessage());
    }

    @Test
    void replaceIllegalFileNameSymbols() {
        assertThat(fileNameHelper.replaceIllegalFileNameSymbols("Space/test:86.pdf")).isEqualTo("Space_test_86.pdf");
    }
}
