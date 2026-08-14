package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkProcessingServiceConnectorTest {

    private static final String BULK_SERVICE_URL = "http://localhost:9070";
    private static final String WEASYPRINT_URL = "http://localhost:9080";
    private static final DocumentConversionParams DEFAULT_PARAMS = DocumentConversionParams.builder().build();

    @Mock
    private Client client;
    @Mock
    private ClientBuilder clientBuilderInstance;
    @Mock
    private WebTarget webTarget;
    @Mock
    private Invocation.Builder invocationBuilder;

    private MockedStatic<ClientBuilder> clientBuilderMockedStatic;
    private BulkProcessingServiceConnector connector;

    @BeforeEach
    void setUp() {
        clientBuilderMockedStatic = mockStatic(ClientBuilder.class);
        // Mock both newClient() and newBuilder() paths
        clientBuilderMockedStatic.when(ClientBuilder::newClient).thenReturn(client);
        clientBuilderMockedStatic.when(ClientBuilder::newBuilder).thenReturn(clientBuilderInstance);
        lenient().when(clientBuilderInstance.connectTimeout(anyLong(), any(TimeUnit.class))).thenReturn(clientBuilderInstance);
        lenient().when(clientBuilderInstance.readTimeout(anyLong(), any(TimeUnit.class))).thenReturn(clientBuilderInstance);
        lenient().when(clientBuilderInstance.build()).thenReturn(client);

        lenient().when(client.target(anyString())).thenReturn(webTarget);
        lenient().when(webTarget.request()).thenReturn(invocationBuilder);
        lenient().when(webTarget.request(anyString())).thenReturn(invocationBuilder);
        lenient().when(webTarget.request(any(jakarta.ws.rs.core.MediaType.class))).thenReturn(invocationBuilder);
        lenient().when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);

        connector = new BulkProcessingServiceConnector(BULK_SERVICE_URL, WEASYPRINT_URL);
    }

    @AfterEach
    void tearDown() {
        clientBuilderMockedStatic.close();
    }

    @Test
    void shouldConvertMergedToPdfWithSingleDocument() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"test-job-id\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "merged-pdf-content".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().fileName("test.pdf").build();
        MergeResult result = connector.convertMergedToPdf(List.of(doc("<html>doc1</html>", null)), params);

        assertThat(result.pdfBytes()).isEqualTo("merged-pdf-content".getBytes());
        verify(invocationBuilder, times(3)).post(any(Entity.class));
    }

    @Test
    void shouldConvertMergedToPdfWithCoverPage() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-with-cover\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "pdf-with-cover".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        MergeResult result = connector.convertMergedToPdf(List.of(doc("<html>content</html>", "<html>cover</html>")), params);

        assertThat(result.pdfBytes()).isEqualTo("pdf-with-cover".getBytes());
    }

    @Test
    void shouldConvertMergedToPdfWithMultipleDocuments() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"multi-job\"}");
        Response addResponse1 = mockResponse(200, "{\"status\":\"accepted\"}");
        Response addResponse2 = mockResponse(202, "{\"status\":\"accepted\"}");
        Response addResponse3 = mockResponse(200, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "multi-pdf".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse1)
                .thenReturn(addResponse2)
                .thenReturn(addResponse3)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        MergeResult result = connector.convertMergedToPdf(List.of(
                doc("<html>doc1</html>", null),
                doc("<html>doc2</html>", null),
                doc("<html>doc3</html>", "<html>cover3</html>")), params);

        assertThat(result.pdfBytes()).isEqualTo("multi-pdf".getBytes());
        verify(invocationBuilder, times(5)).post(any(Entity.class));
    }

    @Test
    void shouldThrowWhenStartMergeJobFails() {
        Response errorResponse = mockResponse(500, "Internal Server Error");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(errorResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();

        assertThatThrownBy(() -> connector.convertMergedToPdf(List.of(doc("<html></html>", null)), params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to start merge job");
    }

    @Test
    void shouldContinueOnAddFailureAndReportCount() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-id\"}");
        Response addFailResponse = mockResponse(500, "Conversion failed");
        Response addOkResponse = mockResponse(200, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "partial-pdf".getBytes(), "1");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addFailResponse)
                .thenReturn(addOkResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        MergeResult result = connector.convertMergedToPdf(List.of(
                doc("<html>fail</html>", null),
                doc("<html>ok</html>", null)), params);

        assertThat(result.failedDocumentCount()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenFinishMergeJobFails() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-id\"}");
        Response addResponse = mockResponse(200, "ok");
        Response finishErrorResponse = mockResponse(500, "Merge failed");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishErrorResponse);

        Response deleteResponse = mockResponse(204, "");
        when(invocationBuilder.delete()).thenReturn(deleteResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();

        assertThatThrownBy(() -> connector.convertMergedToPdf(List.of(doc("<html></html>", null)), params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to finish merge job");
    }

    @Test
    void shouldAbortMergeWhenThreadInterrupted() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"interrupted-job\"}");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(startResponse);

        Response deleteResponse = mockResponse(204, "");
        when(invocationBuilder.delete()).thenReturn(deleteResponse);

        Thread.currentThread().interrupt();

        MergeJobStartParams params = MergeJobStartParams.builder().build();

        assertThatThrownBy(() -> connector.convertMergedToPdf(
                List.of(doc("<html>doc1</html>", null), doc("<html>doc2</html>", null)), params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was cancelled");

        Thread.interrupted();
    }

    @Test
    void shouldDeleteJobOnFailure() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"fail-job\"}");
        Response finishErrorResponse = mockResponse(500, "Merge failed");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(finishErrorResponse);

        Response deleteResponse = mockResponse(204, "");
        when(invocationBuilder.delete()).thenReturn(deleteResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();

        assertThatThrownBy(() -> connector.convertMergedToPdf(List.of(doc("<html></html>", null)), params))
                .isInstanceOf(IllegalStateException.class);

        verify(invocationBuilder).delete();
    }

    private MergeDocumentData doc(String html, String coverPageHtml) {
        return new MergeDocumentData(html, coverPageHtml, DEFAULT_PARAMS);
    }

    private Response mockResponse(int status, String body) {
        Response response = mock(Response.class);
        lenient().when(response.getStatus()).thenReturn(status);
        lenient().when(response.readEntity(String.class)).thenReturn(body);
        return response;
    }

    private Response mockPdfResponse(int status, byte[] body) {
        return mockPdfResponse(status, body, null);
    }

    private Response mockPdfResponse(int status, byte[] body, String failedCount) {
        Response response = mock(Response.class);
        lenient().when(response.getStatus()).thenReturn(status);
        lenient().when(response.readEntity(java.io.InputStream.class)).thenReturn(new ByteArrayInputStream(body));
        lenient().when(response.getHeaderString("X-Documents-Failed")).thenReturn(failedCount);
        return response;
    }
}
