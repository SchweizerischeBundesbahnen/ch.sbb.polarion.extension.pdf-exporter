package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
class LiveDocAdapterGetDocumentRendererTest {

    @Mock
    private InternalReadOnlyTransaction transaction;

    @Mock
    private ProxyDocument document;

    @Test
    void testGetDocumentRendererPassesQueryAndLanguageParameters() throws Exception {
        List<DocumentRendererParameters> capturedParameters = new ArrayList<>();

        try (MockedConstruction<ModifiedDocumentRenderer> ignored = mockConstruction(ModifiedDocumentRenderer.class,
                (mock, context) -> capturedParameters.add((DocumentRendererParameters) context.arguments().get(3)))) {

            ExportParams exportParams = ExportParams.builder()
                    .urlQueryParameters(Map.of(
                            LiveDocAdapter.URL_QUERY_PARAM_QUERY, "type:requirement",
                            LiveDocAdapter.URL_QUERY_PARAM_LANGUAGE, "de"
                    ))
                    .build();

            LiveDocAdapter.getDocumentRenderer(exportParams, transaction, document);

            assertEquals(1, capturedParameters.size());
            DocumentRendererParameters parameters = capturedParameters.get(0);
            assertEquals("type:requirement", getFieldValue(parameters, "query"));
            assertEquals("de", getFieldValue(parameters, "language"));
        }
    }

    @Test
    void testGetDocumentRendererWithNullUrlQueryParameters() throws Exception {
        List<DocumentRendererParameters> capturedParameters = new ArrayList<>();

        try (MockedConstruction<ModifiedDocumentRenderer> ignored = mockConstruction(ModifiedDocumentRenderer.class,
                (mock, context) -> capturedParameters.add((DocumentRendererParameters) context.arguments().get(3)))) {

            ExportParams exportParams = ExportParams.builder()
                    .urlQueryParameters(null)
                    .build();

            LiveDocAdapter.getDocumentRenderer(exportParams, transaction, document);

            assertEquals(1, capturedParameters.size());
            DocumentRendererParameters parameters = capturedParameters.get(0);
            assertNull(getFieldValue(parameters, "query"));
            assertNull(getFieldValue(parameters, "language"));
        }
    }

    @Test
    void testGetDocumentRendererWithOnlyQueryParameter() throws Exception {
        List<DocumentRendererParameters> capturedParameters = new ArrayList<>();

        try (MockedConstruction<ModifiedDocumentRenderer> ignored = mockConstruction(ModifiedDocumentRenderer.class,
                (mock, context) -> capturedParameters.add((DocumentRendererParameters) context.arguments().get(3)))) {

            ExportParams exportParams = ExportParams.builder()
                    .urlQueryParameters(Map.of(LiveDocAdapter.URL_QUERY_PARAM_QUERY, "status:approved"))
                    .build();

            LiveDocAdapter.getDocumentRenderer(exportParams, transaction, document);

            assertEquals(1, capturedParameters.size());
            DocumentRendererParameters parameters = capturedParameters.get(0);
            assertEquals("status:approved", getFieldValue(parameters, "query"));
            assertNull(getFieldValue(parameters, "language"));
        }
    }

    @Test
    void testGetDocumentRendererWithOnlyLanguageParameter() throws Exception {
        List<DocumentRendererParameters> capturedParameters = new ArrayList<>();

        try (MockedConstruction<ModifiedDocumentRenderer> ignored = mockConstruction(ModifiedDocumentRenderer.class,
                (mock, context) -> capturedParameters.add((DocumentRendererParameters) context.arguments().get(3)))) {

            ExportParams exportParams = ExportParams.builder()
                    .urlQueryParameters(Map.of(LiveDocAdapter.URL_QUERY_PARAM_LANGUAGE, "fr"))
                    .build();

            LiveDocAdapter.getDocumentRenderer(exportParams, transaction, document);

            assertEquals(1, capturedParameters.size());
            DocumentRendererParameters parameters = capturedParameters.get(0);
            assertNull(getFieldValue(parameters, "query"));
            assertEquals("fr", getFieldValue(parameters, "language"));
        }
    }

    @Test
    void testGetDocumentRendererWithEmptyUrlQueryParameters() throws Exception {
        List<DocumentRendererParameters> capturedParameters = new ArrayList<>();

        try (MockedConstruction<ModifiedDocumentRenderer> ignored = mockConstruction(ModifiedDocumentRenderer.class,
                (mock, context) -> capturedParameters.add((DocumentRendererParameters) context.arguments().get(3)))) {

            ExportParams exportParams = ExportParams.builder()
                    .urlQueryParameters(Map.of())
                    .build();

            LiveDocAdapter.getDocumentRenderer(exportParams, transaction, document);

            assertEquals(1, capturedParameters.size());
            DocumentRendererParameters parameters = capturedParameters.get(0);
            assertNull(getFieldValue(parameters, "query"));
            assertNull(getFieldValue(parameters, "language"));
        }
    }

    private Object getFieldValue(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
