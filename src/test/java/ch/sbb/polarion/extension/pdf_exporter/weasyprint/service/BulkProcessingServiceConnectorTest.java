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
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
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
    @Mock
    private ApiKeyProvider apiKeyProvider;
    @Mock
    private PolarionTokenIssuer tokenIssuer;

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

        // Default: no API key configured, which keeps the pre-existing behavior of these tests.
        lenient().when(apiKeyProvider.getApiKey()).thenReturn(null);

        connector = new BulkProcessingServiceConnector(BULK_SERVICE_URL, WEASYPRINT_URL, apiKeyProvider);
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

        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
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

        // The first add is refused (500), so that document never reached the server - an upload failure the
        // server does not know about. Separately the server reports one rendered-but-failed document
        // (X-Documents-Failed=1). Disjoint sets, so two documents were dropped from the merged PDF.
        assertThat(result.failedDocumentCount()).isEqualTo(2);
    }

    @Test
    void shouldNotDoubleCountServerReportedRenderFailures() {
        // A document the server received but could not render is accepted (202) and recorded server-side,
        // surfacing only in X-Documents-Failed at finish. It must be counted once, not once per side.
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-id\"}");
        Response addAccepted = mockResponse(202, "{\"status\":\"accepted\"}");
        Response addFailedButRecorded = mockResponse(202, "{\"status\":\"failed\"}");
        Response finishResponse = mockPdfResponse(200, "partial-pdf".getBytes(), "1");

        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addFailedButRecorded)
                .thenReturn(addAccepted)
                .thenReturn(finishResponse);

        MergeResult result = connector.convertMergedToPdf(List.of(
                doc("<html>fail</html>", null),
                doc("<html>ok</html>", null)), MergeJobStartParams.builder().build());

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

        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
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

        List<MergeDocumentData> documents = List.of(doc("<html>doc1</html>", null), doc("<html>doc2</html>", null));
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
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

        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(IllegalStateException.class);

        verify(invocationBuilder).delete();
    }

    @Test
    void shouldSendApiKeyHeaderOverHttps() {
        when(apiKeyProvider.getApiKey()).thenReturn("secret");
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);

        Response startResponse = mockResponse(201, "{\"jobId\":\"job\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "merged-pdf-content".getBytes());
        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        BulkProcessingServiceConnector httpsConnector = new BulkProcessingServiceConnector("https://localhost:9070", WEASYPRINT_URL, apiKeyProvider);
        httpsConnector.convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        verify(invocationBuilder, atLeastOnce()).header("X-API-Key", "secret");
    }

    private BulkProcessingServiceConnector connectorForUser(java.util.function.Supplier<String> user) {
        return new BulkProcessingServiceConnector(BULK_SERVICE_URL, WEASYPRINT_URL, apiKeyProvider, tokenIssuer, user);
    }

    private void stubSuccessfulMerge() {
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-1\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "merged-pdf-content".getBytes());
        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);
    }

    @Test
    void shouldSendPolarionTokenOfTheUserOnEveryCall() {
        when(tokenIssuer.issue(eq("alice"), isNull())).thenReturn("token-of-start");
        when(tokenIssuer.issue("alice", "job-1")).thenReturn("token-of-job-1");
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        stubSuccessfulMerge();

        connectorForUser(() -> "alice").convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        // start has no job yet and carries none, add and finish carry the job they address
        verify(invocationBuilder, times(1)).header("X-Polarion-Token", "token-of-start");
        verify(invocationBuilder, times(2)).header("X-Polarion-Token", "token-of-job-1");
    }

    @Test
    void shouldMakeANewTokenForEveryCall() {
        when(tokenIssuer.issue(anyString(), any())).thenReturn("token");
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        stubSuccessfulMerge();

        connectorForUser(() -> "alice").convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        // start, add, finish: a token lives for minutes, a long export must not run out of it
        verify(tokenIssuer, times(3)).issue(anyString(), any());
    }

    @Test
    void shouldSendPolarionTokenWhenCleaningUpAFailedJob() {
        when(tokenIssuer.issue(anyString(), any())).thenAnswer(invocation -> "token-for-" + invocation.getArgument(1));
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-1\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockResponse(500, "boom");
        Response deleteResponse = mockResponse(204, "");
        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);
        when(invocationBuilder.delete()).thenReturn(deleteResponse);

        BulkProcessingServiceConnector connector = connectorForUser(() -> "alice");
        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        MergeJobStartParams params = MergeJobStartParams.builder().build();
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params)).isInstanceOf(IllegalStateException.class);

        // add, finish and the delete which cleans up all name the job
        verify(invocationBuilder, times(3)).header("X-Polarion-Token", "token-for-job-1");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {" "})
    void shouldSendNoPolarionTokenWithoutUser(String noUser) {
        stubSuccessfulMerge();

        connectorForUser(() -> noUser).convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        verify(tokenIssuer, never()).issue(anyString(), any());
        verify(invocationBuilder, never()).header(eq("X-Polarion-Token"), any());
    }

    @Test
    void shouldGoOnWithoutTokenWhenPolarionGivesNone() {
        when(tokenIssuer.issue(anyString(), any())).thenReturn(null);
        stubSuccessfulMerge();

        MergeResult result = connectorForUser(() -> "alice").convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        // a service which does not check tokens needs none: the export is not stopped for the lack of one
        assertThat(result.pdfBytes()).isEqualTo("merged-pdf-content".getBytes());
        verify(invocationBuilder, never()).header(eq("X-Polarion-Token"), any());
    }

    @Test
    void shouldNameTheTokenWhenTheServiceRefusesIt() {
        when(tokenIssuer.issue(anyString(), any())).thenReturn("token");
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        Response refused = mockResponse(401, "{\"detail\":\"Invalid Polarion token\"}");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(refused);

        BulkProcessingServiceConnector connector = connectorForUser(() -> "alice");
        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        MergeJobStartParams params = MergeJobStartParams.builder().build();

        // not the message about the API key: its fix is elsewhere
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("refused the Polarion token")
                .hasMessageContaining("POLARION_JWKS_URL")
                .hasMessageNotContaining("API key");
    }

    @Test
    void shouldKeepTheApiKeyMessageWhenTheKeyIsWhatTheServiceRefuses() {
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(true, "{\"detail\":\"Invalid or missing API key\"}")).contains("rejected the configured API key");
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(false, "{\"detail\":\"Invalid or missing API key\"}")).contains("requires an API key");
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(true)).contains("rejected the configured API key");
    }

    @Test
    void shouldRejectApiKeyOverPlainHttp() {
        when(apiKeyProvider.getApiKey()).thenReturn("secret");

        // The connector from setUp is named over http, so the key must never leave.
            List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        MergeJobStartParams params = MergeJobStartParams.builder().build();
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("not sent over plain http");
    }

    @Test
    void shouldReportMissingKeyOnUnauthorized() {
        // apiKeyProvider returns null (default), so a 401 means the service wants a key we do not have.
        Response startResponse = mockResponse(401, "Unauthorized");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(startResponse);

        List<MergeDocumentData> documents = List.of(doc("<html></html>", null));
        MergeJobStartParams params = MergeJobStartParams.builder().build();
        assertThatThrownBy(() -> connector.convertMergedToPdf(documents, params))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("requires an API key");
    }

    @Test
    void unauthorizedMessageDistinguishesTheTwoCases() {
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(true)).contains("rejected the configured API key");
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(false)).contains("requires an API key");
    }

    @Test
    void unauthorizedMessageSaysWhenNoTokenCouldBeIssued() {
        String missing = "{\"detail\":\"Missing Polarion token\"}";
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(true, false, missing)).contains("none could be issued");
        assertThat(BulkProcessingServiceConnector.unauthorizedMessage(true, true, missing)).contains("POLARION_JWKS_URL");
    }

    @Test
    void shouldGoOnWithoutTokenWhenTheCurrentUserCannotBeFoundOut() {
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        stubSuccessfulMerge();

        connectorForUser(() -> {
            throw new IllegalStateException("no platform");
        }).convertMergedToPdf(List.of(doc("<html></html>", null)), MergeJobStartParams.builder().build());

        verify(invocationBuilder, never()).header(eq("X-Polarion-Token"), any());
    }

    @Test
    void shouldSendTheTokenOfTheJobWithADocumentWhichEmbedsFiles(@TempDir Path tempDir) throws IOException {
        Path notes = Files.writeString(tempDir.resolve("notes.txt"), "notes");
        when(tokenIssuer.issue(eq("alice"), isNull())).thenReturn("token-of-start");
        when(tokenIssuer.issue("alice", "job-1")).thenReturn("token-of-job-1");
        lenient().when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        stubSuccessfulMerge();

        connectorForUser(() -> "alice").convertMergedToPdf(
                List.of(new MergeDocumentData("<html>with files</html>", null, DEFAULT_PARAMS, List.of(notes))), MergeJobStartParams.builder().build());

        verify(client).target(BULK_SERVICE_URL + "/api/convert/job-1/add-with-attachments");
        verify(invocationBuilder, times(1)).header("X-Polarion-Token", "token-of-start");
        verify(invocationBuilder, times(2)).header("X-Polarion-Token", "token-of-job-1");
    }

    @Test
    void shouldAddADocumentWhichEmbedsFilesAsAMultipartForm(@TempDir Path tempDir) throws IOException {
        Path notes = Files.writeString(tempDir.resolve("notes.txt"), "notes");
        Response startResponse = mockResponse(201, "{\"jobId\":\"job-with-files\"}");
        Response addResponse = mockResponse(202, "{\"status\":\"accepted\"}");
        Response finishResponse = mockPdfResponse(200, "merged".getBytes());
        when(invocationBuilder.post(any(Entity.class)))
                .thenReturn(startResponse)
                .thenReturn(addResponse)
                .thenReturn(addResponse)
                .thenReturn(finishResponse);

        connector.convertMergedToPdf(List.of(
                new MergeDocumentData("<html>with files</html>", null, DEFAULT_PARAMS, List.of(notes)),
                doc("<html>without files</html>", null)), MergeJobStartParams.builder().build());

        verify(client).target(BULK_SERVICE_URL + "/api/convert/job-with-files/add-with-attachments");
        verify(client).target(BULK_SERVICE_URL + "/api/convert/job-with-files/add");
        verify(webTarget).register(MultiPartFeature.class);
    }

    @Test
    void shouldSendTheFieldsAndTheFilesOfADocumentAsFormParts(@TempDir Path tempDir) throws IOException {
        Path notes = Files.writeString(tempDir.resolve("notes.txt"), "notes");
        DocumentConversionParams params = DocumentConversionParams.builder().pdfVariant(PdfVariant.PDF_A_4F.toWeasyPrintParameter()).build();

        try (FormDataMultiPart multipart = BulkProcessingServiceConnector.toMultiPart(new MergeDocumentData("<html>Zürich</html>", "<html>cover</html>", params, List.of(notes)))) {
            assertThat(text(multipart.getField("html"))).isEqualTo("<html>Zürich</html>");
            assertThat(text(multipart.getField("coverPageHtml"))).isEqualTo("<html>cover</html>");
            assertThat(text(multipart.getField("params"))).contains("\"pdfVariant\":\"pdf/a-4f\"");
            FileDataBodyPart file = (FileDataBodyPart) multipart.getField("files");
            assertThat(file.getFileEntity()).isEqualTo(notes.toFile());
            assertThat(file.getContentDisposition().getFileName()).isEqualTo("notes.txt");
        }
    }

    @Test
    void shouldLeaveOutTheCoverPageOfADocumentWithoutOne(@TempDir Path tempDir) throws IOException {
        Path notes = Files.writeString(tempDir.resolve("notes.txt"), "notes");

        try (FormDataMultiPart multipart = BulkProcessingServiceConnector.toMultiPart(new MergeDocumentData("<html></html>", null, DEFAULT_PARAMS, List.of(notes)))) {
            assertThat(multipart.getField("coverPageHtml")).isNull();
            assertThat(multipart.getFields("files")).hasSize(1);
        }
    }

    private static String text(FormDataBodyPart part) {
        return new String((byte[]) part.getEntity(), StandardCharsets.UTF_8);
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
