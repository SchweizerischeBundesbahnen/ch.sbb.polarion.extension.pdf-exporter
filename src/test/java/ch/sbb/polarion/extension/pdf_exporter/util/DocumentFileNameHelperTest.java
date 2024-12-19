package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf_exporter.test_extensions.DocumentDataFactoryMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith({
        MockitoExtension.class,
        TransactionalExecutorExtension.class,
        PlatformContextMockExtension.class,
        CurrentContextExtension.class,
        DocumentDataFactoryMockExtension.class
})
class DocumentFileNameHelperTest {

    @Mock
    private VelocityEvaluator velocityEvaluator;

    @InjectMocks
    private DocumentFileNameHelper fileNameHelper;

    @InjectMocks
    private DocumentDataFactoryMockExtension documentDataFactoryMockExtension;

    @Test
    void evaluateVelocity() {
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, mock(IModule.class))
                .id(new LiveDocId(new DocumentProject("testProjectId", "Test Project"), "testSpaceId", "testDocumentId"))
                .title("Test Title")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();
        FileNameTemplateModel settingOneModel = FileNameTemplateModel.builder()
                .documentNameTemplate("$projectName $document.moduleFolder $document.moduleName")
                .build();

        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);

        String result = fileNameHelper.evaluateVelocity(documentData, settingOneModel.getDocumentNameTemplate());
        assertThat(result).endsWith(".pdf");
    }

    @Test
    void getDocumentFileNameWithMissingProjectIdForLiveDocOrTestRun() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId(null)
                .documentType(DocumentType.LIVE_DOC)
                .build();
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileNameHelper.getDocumentFileName(exportParams));
        assertEquals("Project ID must be provided for LiveDoc or TestRun export", exception.getMessage());
    }

    @Test
    void getDocumentFileNameWithUnsupportedDocumentType() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .documentType(DocumentType.BASELINE_COLLECTION)
                .build();
        DocumentData<IBaselineCollection> documentDataMock = mock(DocumentData.class);
        documentDataFactoryMockExtension.register(exportParams, documentDataMock);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fileNameHelper.getDocumentFileName(exportParams));
        assertEquals("Unsupported document type: BASELINE_COLLECTION", exception.getMessage());
    }

    @Test
    void getDocumentFileName() {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .projectId("testProjectId")
                .locationPath("testSpaceId/testDocumentId")
                .documentType(DocumentType.LIVE_DOC)
                .build();
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, mock(IModule.class))
                .id(new LiveDocId(new DocumentProject("testProjectId", "Test Project"), "testSpaceId", "testDocumentId"))
                .title("Test Title")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();
        documentDataFactoryMockExtension.register(exportParams, documentData);

        FileNameTemplateSettings fileNameTemplateSettings = new FileNameTemplateSettings();
        FileNameTemplateSettings fileNameTemplateSettingsSpy = spy(fileNameTemplateSettings);
        doReturn(fileNameTemplateSettings.defaultValues()).when(fileNameTemplateSettingsSpy).read(anyString(), any(), any());

        NamedSettingsRegistry.INSTANCE.register(List.of(fileNameTemplateSettingsSpy));

        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);

        // Act & Assert
        String documentFileName = fileNameHelper.getDocumentFileName(exportParams);
        assertThat(documentFileName).endsWith(".pdf");
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
