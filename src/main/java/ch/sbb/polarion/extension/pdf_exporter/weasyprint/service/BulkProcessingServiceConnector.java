package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfPostProcessor;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public byte[] convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params) {
        params.setWeasyPrintServiceUrl(weasyPrintServiceBaseUrl);
        String jobId = startMergeJob(params);
        byte[] pdfBytes;
        try {
            for (MergeDocumentData doc : documents) {
                if (doc.coverPageHtml() != null) {
                    addDocumentWithCoverPageToJob(jobId, doc.htmlContent(), doc.coverPageHtml());
                } else {
                    addDocumentToJob(jobId, doc.htmlContent());
                }
            }
            pdfBytes = finishMergeJob(jobId);
        } catch (Exception e) {
            logger.error(String.format("Merge job '%s' failed, attempting cleanup", jobId), e);
            deleteMergeJob(jobId);
            throw e;
        }

        return pdfPostProcessor.postProcess(pdfBytes, PdfVariant.fromWeasyPrintParameter(params.getPdfVariant()), null);
    }

    private @NotNull String startMergeJob(@NotNull MergeJobStartParams params) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
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
                    return response.readEntity(String.class).trim().replace("\"", "");
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

    private void addDocumentToJob(@NotNull String jobId, @NotNull String htmlContent) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId + "/add");

            try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(htmlContent, MediaType.TEXT_HTML))) {
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

    private void addDocumentWithCoverPageToJob(@NotNull String jobId, @NotNull String htmlContent, @NotNull String coverPageHtml) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId + "/add-with-cover");

            String jsonBody;
            try {
                jsonBody = new ObjectMapper().writeValueAsString(Map.of("html", htmlContent, "coverPageHtml", coverPageHtml));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not serialize add-with-cover request", e);
            }

            try (Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jsonBody, MediaType.APPLICATION_JSON))) {
                if (response.getStatus() != Response.Status.OK.getStatusCode()
                        && response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                    String errorMessage = response.readEntity(String.class);
                    throw new IllegalStateException(String.format(
                            "Failed to add document with cover page to merge job '%s'. Status: %s, Message: [%s]",
                            jobId, response.getStatus(), errorMessage));
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private byte[] finishMergeJob(@NotNull String jobId) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(bulkProcessingServiceBaseUrl + MERGE_API_PREFIX + jobId + "/stop");

            try (Response response = webTarget.request("application/pdf")
                    .post(Entity.entity("", MediaType.TEXT_PLAIN))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    InputStream inputStream = response.readEntity(InputStream.class);
                    try {
                        return inputStream.readAllBytes();
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

    @SuppressWarnings("java:S1166") // Exception intentionally caught for best-effort cleanup
    private void deleteMergeJob(@NotNull String jobId) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
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
