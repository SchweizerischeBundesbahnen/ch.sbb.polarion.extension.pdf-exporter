package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentFileNameHelperTest {
    @Mock
    VelocityEvaluator velocityEvaluator;

    @InjectMocks
    DocumentFileNameHelper fileNameHelper;

    @Test
    void evaluateVelocity() {
        DocumentData<IModule> documentData = DocumentData.<IModule>builder()
                .projectName("Test Project")
                .documentType(DocumentType.DOCUMENT)
                .documentObject(mock(IModule.class))
                .build();
        FileNameTemplateModel settingOneModel = FileNameTemplateModel.builder()
                .documentNameTemplate("$projectName $document.moduleFolder $document.moduleName")
                .build();

        when(velocityEvaluator.evaluateVelocityExpressions(eq(documentData), anyString())).thenAnswer(a -> a.getArguments()[1]);

        String result = fileNameHelper.evaluateVelocity(documentData, settingOneModel.getDocumentNameTemplate());
        assertThat(result).contains("pdf");
    }

    @Test
    void replaceIllegalFileNameSymbols() {
        assertThat(fileNameHelper.replaceIllegalFileNameSymbols("Space/test:86.pdf")).isEqualTo("Space_test_86.pdf");
    }
}