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
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class BulkProcessingServiceConnector implements BulkProcessingConnector {
    private static final Logger logger = Logger.getLogger(BulkProcessingServiceConnector.class);

    private static final String MERGE_API_PREFIX = "/api/convert/";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 600_000;

    private final @NotNull String bulkProcessingServiceBaseUrl;
    private final @NotNull String weasyPrintServiceBaseUrl;
    private final PdfPostProcessor pdfPostProcessor = new PdfPostProcessor();

    public BulkProcessingServiceConnector() {
        this(PdfExporterExtensionConfiguration.getInstance().getBulkProcessingService(),
                PdfExporterExtensionConfiguration.getInstance().getWeasyPrintService());
    }

    public BulkProcessingServiceConnector(@NotNull String bulkProcessingServiceBaseUrl, @NotNull String weasyPrintServiceBaseUrl) {
        this.bulkProcessingServiceBaseUrl = bulkProcessingServiceBaseUrl;
        this.weasyPrintServiceBaseUrl = weasyPrintServiceBaseUrl;
    }

    private static @NotNull Client createClient() {
        return ClientBuilder.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Converts multiple documents to PDFs and merges them into one.
     * On success, the job transitions to COMPLETED and remains on disk until TTL-based cleanup removes it.
     * On failure, the job is deleted immediately via best-effort DELETE call.
     */
    @Override
    public MergeResult convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params) {
        String jobId = startMergeJob(params);
        int failedCount = 0;
        try {
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
            FinishResult finishResult = finishMergeJob(jobId);
            failedCount = finishResult.failedCount > 0 ? finishResult.failedCount : failedCount;
            byte[] pdfBytes = pdfPostProcessor.postProcess(finishResult.pdfBytes, PdfVariant.fromWeasyPrintParameter(params.getPdfVariant()), null);
            return new MergeResult(pdfBytes, failedCount);
        } catch (Exception e) {
            logger.error(String.format("Merge job '%s' failed, attempting cleanup", jobId), e);
            deleteMergeJob(jobId);
            throw e;
        }
    }

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

            try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()
                        || response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                    String responseBody = response.readEntity(String.class);
                    try {
                        return new ObjectMapper().readTree(responseBody).get("jobId").asText();
                    } catch (Exception e) {
                        throw new IllegalStateException("Could not parse job ID from start response: " + responseBody, e);
                    }
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

            try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON))) {
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

            try (Response response = webTarget.request("application/pdf")
                    .post(Entity.entity("", MediaType.TEXT_PLAIN))) {
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
            client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId).request().delete().close();
        } catch (Exception cleanup) {
            logger.warn(String.format("Failed to delete merge job '%s': %s", jobId, cleanup.getMessage()));
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
