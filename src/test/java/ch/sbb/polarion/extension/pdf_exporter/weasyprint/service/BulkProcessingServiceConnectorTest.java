package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private Client client;
    @Mock
    private WebTarget webTarget;
    @Mock
    private Invocation.Builder invocationBuilder;

    private MockedStatic<ClientBuilder> clientBuilderMockedStatic;
    private BulkProcessingServiceConnector connector;

    @BeforeEach
    void setUp() {
        clientBuilderMockedStatic = mockStatic(ClientBuilder.class);
        clientBuilderMockedStatic.when(ClientBuilder::newClient).thenReturn(client);
        lenient().when(client.target(anyString())).thenReturn(webTarget);
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
        // Start job → returns job ID
        Response startResponse = mockResponse(201, "\"test-job-id\"");
        // Add document → accepted
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        // Finish job → returns PDF bytes
        Response finishResponse = mockPdfResponse(200, "merged-pdf-content".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().fileName("test.pdf").build();
        List<MergeDocumentData> documents = List.of(new MergeDocumentData("<html>doc1</html>", null));

        byte[] result = connector.convertMergedToPdf(documents, params);

        assertThat(result).isEqualTo("merged-pdf-content".getBytes());
        assertThat(params.getWeasyPrintServiceUrl()).isEqualTo(WEASYPRINT_URL);
        verify(invocationBuilder, times(3)).post(any(Entity.class));
    }

    @Test
    void shouldConvertMergedToPdfWithCoverPage() {
        Response startResponse = mockResponse(201, "\"job-with-cover\"");
        Response addWithCoverResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "pdf-with-cover".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addWithCoverResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(
                new MergeDocumentData("<html>content</html>", "<html>cover</html>"));

        byte[] result = connector.convertMergedToPdf(documents, params);

        assertThat(result).isEqualTo("pdf-with-cover".getBytes());
        verify(client, times(3)).target(anyString());
    }

    @Test
    void shouldConvertMergedToPdfWithMultipleDocuments() {
        Response startResponse = mockResponse(201, "\"multi-job\"");
        Response addResponse1 = mockResponse(200, "{\"status\":\"accepted\"}");
        Response addResponse2 = mockResponse(202, "{\"status\":\"accepted\"}");
        Response addWithCoverResponse = mockResponse(200, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "multi-pdf".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse1)
                .thenReturn(addResponse2)
                .thenReturn(addWithCoverResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(
                new MergeDocumentData("<html>doc1</html>", null),
                new MergeDocumentData("<html>doc2</html>", null),
                new MergeDocumentData("<html>doc3</html>", "<html>cover3</html>"));

        byte[] result = connector.convertMergedToPdf(documents, params);

        assertThat(result).isEqualTo("multi-pdf".getBytes());
        // start + 3 adds + finish = 5 posts
        verify(invocationBuilder, times(5)).post(any(Entity.class));
    }

    @Test
    void shouldThrowWhenStartMergeJobFails() {
        Response errorResponse = mockResponse(500, "Internal Server Error");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(errorResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(new MergeDocumentData("<html></html>", null));

        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to start merge job");
    }

    @Test
    void shouldThrowWhenAddDocumentFails() {
        Response startResponse = mockResponse(201, "\"job-id\"");
        Response addErrorResponse = mockResponse(500, "Conversion failed");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addErrorResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(new MergeDocumentData("<html></html>", null));

        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to add document to merge job");
    }

    @Test
    void shouldThrowWhenAddDocumentWithCoverFails() {
        Response startResponse = mockResponse(201, "\"job-id\"");
        Response addErrorResponse = mockResponse(500, "Cover conversion failed");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addErrorResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(new MergeDocumentData("<html></html>", "<cover/>"));

        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to add document with cover page to merge job");
    }

    @Test
    void shouldThrowWhenFinishMergeJobFails() {
        Response startResponse = mockResponse(201, "\"job-id\"");
        Response addResponse = mockResponse(200, "ok");
        Response finishErrorResponse = mockResponse(500, "Merge failed");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishErrorResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        List<MergeDocumentData> documents = List.of(new MergeDocumentData("<html></html>", null));

        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to finish merge job");
    }

    @Test
    void shouldSetWeasyPrintServiceUrlOnParams() {
        Response startResponse = mockResponse(201, "\"job-id\"");
        Response addResponse = mockResponse(200, "ok");
        Response finishResponse = mockPdfResponse(200, "pdf".getBytes());

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        MergeJobStartParams params = MergeJobStartParams.builder().build();
        assertThat(params.getWeasyPrintServiceUrl()).isNull();

        connector.convertMergedToPdf(List.of(new MergeDocumentData("<html/>", null)), params);

        assertThat(params.getWeasyPrintServiceUrl()).isEqualTo(WEASYPRINT_URL);
    }

    private Response mockResponse(int status, String body) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        lenient().when(response.readEntity(String.class)).thenReturn(body);
        return response;
    }

    private Response mockPdfResponse(int status, byte[] body) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.readEntity(java.io.InputStream.class)).thenReturn(new ByteArrayInputStream(body));
        return response;
    }
}
