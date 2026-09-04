package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfPostProcessor;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.BulkProcessingServiceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BulkProcessingServiceConnector implements BulkProcessingConnector {
    private static final Logger logger = Logger.getLogger(BulkProcessingServiceConnector.class);

    private static final String MERGE_API_PREFIX = "/api/convert/";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 600_000;

    private final @NotNull String bulkProcessingServiceBaseUrl;
    private final @NotNull String weasyPrintServiceBaseUrl;
    private final @NotNull ApiKeyProvider apiKeyProvider;
    private final PdfPostProcessor pdfPostProcessor = new PdfPostProcessor();

    public BulkProcessingServiceConnector() {
        this(PdfExporterExtensionConfiguration.getInstance().getBulkProcessingService(),
                PdfExporterExtensionConfiguration.getInstance().getWeasyPrintService());
    }

    public BulkProcessingServiceConnector(@NotNull String bulkProcessingServiceBaseUrl, @NotNull String weasyPrintServiceBaseUrl) {
        this(bulkProcessingServiceBaseUrl, weasyPrintServiceBaseUrl,
                new ApiKeyProvider(() -> PdfExporterExtensionConfiguration.getInstance().getBulkProcessingApiKeySecret(), "bulk processing service"));
    }

    public BulkProcessingServiceConnector(@NotNull String bulkProcessingServiceBaseUrl, @NotNull String weasyPrintServiceBaseUrl, @NotNull ApiKeyProvider apiKeyProvider) {
        this.bulkProcessingServiceBaseUrl = bulkProcessingServiceBaseUrl;
        this.weasyPrintServiceBaseUrl = weasyPrintServiceBaseUrl;
        this.apiKeyProvider = apiKeyProvider;
    }

    private static @NotNull Client createClient() {
        return ClientBuilder.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Builds the request, carrying the API key when one is configured.
     * <p>
     * A key is a reusable credential, so it is only ever handed to a transport which protects it.
     * Where the service is named over plain http the request is refused instead: sending the key
     * would put it on the wire for anyone on the path to keep.
     *
     * @return the key that was attached, or {@code null} when none is configured
     */
    @VisibleForTesting
    @Nullable String applyApiKey(@NotNull Invocation.Builder builder) {
        String apiKey = apiKeyProvider.getApiKey();
        if (apiKey != null) {
            failOnInsecureTransport();
            builder.header(API_KEY_HEADER, apiKey);
        }
        return apiKey;
    }

    /**
     * Refuses to send the key where the transport does not protect it.
     */
    @VisibleForTesting
    void failOnInsecureTransport() {
        if (!bulkProcessingServiceBaseUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw new UserFriendlyRuntimeException(String.format(
                    "The bulk processing service API key is not sent over plain http. Name the service in '%s' with an https address, or clear '%s' where the service needs no key.",
                    PdfExporterExtensionConfiguration.BULK_PROCESSING_SERVICE, PdfExporterExtensionConfiguration.BULK_PROCESSING_API_KEY_SECRET));
        }
    }

    /**
     * Tells the two ways a 401 is reached apart, since each one has a different fix.
     */
    @VisibleForTesting
    static @NotNull String unauthorizedMessage(boolean apiKeySent) {
        return apiKeySent
                ? "Bulk processing service rejected the configured API key. Check that the Polarion secret named in '" + PdfExporterExtensionConfiguration.BULK_PROCESSING_API_KEY_SECRET + "' holds the key the service was started with."
                : "Bulk processing service requires an API key, none is configured. Name the Polarion secret holding it in '" + PdfExporterExtensionConfiguration.BULK_PROCESSING_API_KEY_SECRET + "'.";
    }

    /**
     * Converts multiple documents to PDFs and merges them into one.
     * On success, the job transitions to COMPLETED and remains on disk until TTL-based cleanup removes it.
     * On failure, the job is deleted immediately via best-effort DELETE call.
     */
    @Override
    public MergeResult convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params) {
        String jobId = startMergeJob(params);
        try {
            int failedCount = addDocumentsToJob(jobId, documents);
            FinishResult finishResult = finishMergeJob(jobId);
            // Two disjoint failure sets are summed. A document the server received but could not render is
            // recorded by the server, which accepts the upload (202) and reports it in X-Documents-Failed at
            // finish, so it is never counted here. addDocumentsToJob only counts documents that never reached
            // the server - a failed upload - which the server therefore does not know about. Summing them is
            // correct precisely because neither side counts the other's failures.
            failedCount += finishResult.failedCount;
            byte[] pdfBytes = pdfPostProcessor.postProcess(finishResult.pdfBytes, PdfVariant.fromWeasyPrintParameter(params.getPdfVariant()), null);
            return new MergeResult(pdfBytes, failedCount);
        } catch (Exception e) {
            logger.error(String.format("Merge job '%s' failed, attempting cleanup", jobId), e);
            deleteMergeJob(jobId);
            throw e;
        }
    }

    /**
     * Uploads every document to the running merge job, returning how many failed to upload. A single upload
     * failure does not abort the merge - the document is skipped and counted - but an interruption does.
     */
    private int addDocumentsToJob(@NotNull String jobId, @NotNull List<MergeDocumentData> documents) {
        int failedCount = 0;
        for (MergeDocumentData doc : documents) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(String.format("Merge job '%s' was cancelled", jobId));
            }
            try {
                addDocumentToJob(jobId, doc.htmlContent(), doc.coverPageHtml(), doc.params());
            } catch (Exception e) {
                failedCount++;
                logger.warn(String.format("Failed to add document to merge job '%s': %s", jobId, e.getMessage()));
            }
        }
        return failedCount;
    }

    // A payload carrier (the merged PDF bytes); it is never compared by value, so the array-aware
    // equals/hashCode/toString java:S6218 asks for would be dead boilerplate here.
    @SuppressWarnings("java:S6218")
    private record FinishResult(byte[] pdfBytes, int failedCount) {}

    private @NotNull String startMergeJob(@NotNull MergeJobStartParams params) {
        Client client = null;
        try {
            client = createClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + "start");

            String jsonBody;
            try {
                jsonBody = new ObjectMapper().writeValueAsString(params);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not serialize merge job start params", e);
            }

            Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON);
            boolean apiKeySent = applyApiKey(builder) != null;
            try (Response response = builder.post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()
                        || response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                    String responseBody = response.readEntity(String.class);
                    try {
                        return new ObjectMapper().readTree(responseBody).get("jobId").asText();
                    } catch (Exception e) {
                        throw new IllegalStateException("Could not parse job ID from start response: " + responseBody, e);
                    }
                } else if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    // user friendly on purpose: the reason has to reach the export dialog, not only the log
                    throw new UserFriendlyRuntimeException(unauthorizedMessage(apiKeySent));
                } else {
                    String errorMessage = response.readEntity(String.class);
                    throw new IllegalStateException(String.format(
                            "Failed to start merge job. Status: %s, Message: [%s]",
                            response.getStatus(), errorMessage));
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private void addDocumentToJob(@NotNull String jobId, @NotNull String htmlContent, @Nullable String coverPageHtml, @NotNull DocumentConversionParams docParams) {
        Client client = null;
        try {
            client = createClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId + "/add");

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("html", htmlContent);
            if (coverPageHtml != null) {
                body.put("coverPageHtml", coverPageHtml);
            }
            body.put("params", docParams);

            String jsonBody;
            try {
                jsonBody = new ObjectMapper().writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not serialize add document request", e);
            }

            Invocation.Builder builder = webTarget.request(MediaType.APPLICATION_JSON);
            boolean apiKeySent = applyApiKey(builder) != null;
            try (Response response = builder.post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON))) {
                if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    throw new UserFriendlyRuntimeException(unauthorizedMessage(apiKeySent));
                }
                if (response.getStatus() != Response.Status.OK.getStatusCode()
                        && response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                    String errorMessage = response.readEntity(String.class);
                    throw new IllegalStateException(String.format(
                            "Failed to add document to merge job '%s'. Status: %s, Message: [%s]",
                            jobId, response.getStatus(), errorMessage));
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private FinishResult finishMergeJob(@NotNull String jobId) {
        Client client = null;
        try {
            client = createClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId + "/finish");

            Invocation.Builder builder = webTarget.request("application/pdf");
            boolean apiKeySent = applyApiKey(builder) != null;
            try (Response response = builder.post(Entity.entity("", MediaType.TEXT_PLAIN))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    InputStream inputStream = response.readEntity(InputStream.class);
                    int failedCount = 0;
                    String failedHeader = response.getHeaderString("X-Documents-Failed");
                    if (failedHeader != null) {
                        try {
                            failedCount = Integer.parseInt(failedHeader);
                        } catch (NumberFormatException ignored) {
                            // ignore
                        }
                    }
                    try {
                        return new FinishResult(inputStream.readAllBytes(), failedCount);
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not read merged PDF response stream", e);
                    }
                } else if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    throw new UserFriendlyRuntimeException(unauthorizedMessage(apiKeySent));
                } else {
                    String errorMessage = response.readEntity(String.class);
                    throw new IllegalStateException(String.format(
                            "Failed to finish merge job '%s'. Status: %s, Message: [%s]",
                            jobId, response.getStatus(), errorMessage));
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public BulkProcessingServiceInfo getVersionInfo() {
        Client client = null;
        try {
            client = createClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + "/version");

            try (Response response = webTarget.request(MediaType.APPLICATION_JSON).get()) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    String responseContent = response.readEntity(String.class);
                    try {
                        return new ObjectMapper().readValue(responseContent, BulkProcessingServiceInfo.class);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Could not parse version response from Bulk Processing Service", e);
                    }
                } else {
                    throw new IllegalStateException("Could not get version info from Bulk Processing Service");
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @SuppressWarnings("java:S1166") // Exception intentionally caught for best-effort cleanup
    private void deleteMergeJob(@NotNull String jobId) {
        Client client = null;
        try {
            client = createClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId);
            Invocation.Builder builder = webTarget.request();
            applyApiKey(builder);
            builder.delete().close();
        } catch (Exception cleanup) {
            logger.warn(String.format("Failed to delete merge job '%s': %s", jobId, cleanup.getMessage()));
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
