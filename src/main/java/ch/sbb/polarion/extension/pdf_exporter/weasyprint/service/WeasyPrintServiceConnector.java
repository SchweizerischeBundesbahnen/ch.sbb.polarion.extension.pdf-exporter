package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.core.util.logging.Logger;
import lombok.Getter;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.polarion.core.util.StringUtils.isEmpty;

public class WeasyPrintServiceConnector implements WeasyPrintConverter {
    private static final Logger logger = Logger.getLogger(WeasyPrintServiceConnector.class);

    private static final String PYTHON_VERSION_HEADER = "Python-Version";
    private static final String WEASYPRINT_VERSION_HEADER = "Weasyprint-Version";
    private static final String WEASYPRINT_SERVICE_VERSION_HEADER = "Weasyprint-Service-Version";

    private static final AtomicReference<String> pythonVersion = new AtomicReference<>();
    private static final AtomicReference<String> weasyPrintVersion = new AtomicReference<>();
    private static final AtomicReference<String> weasyPrintServiceVersion = new AtomicReference<>();

    @Getter
    private final @NotNull String weasyPrintServiceBaseUrl;

    public WeasyPrintServiceConnector() {
        this(PdfExporterExtensionConfiguration.getInstance().getWeasyPrintService());
    }

    public WeasyPrintServiceConnector(@NotNull String weasyPrintServiceBaseUrl) {
        this.weasyPrintServiceBaseUrl = weasyPrintServiceBaseUrl;
    }

    @Override
    public byte[] convertToPdf(@NotNull String htmlPage, @NotNull WeasyPrintOptions weasyPrintOptions) {
        return convertToPdf(htmlPage, weasyPrintOptions, null);
    }

    @Override
    public byte[] convertToPdf(@NotNull String htmlPage, @NotNull WeasyPrintOptions weasyPrintOptions, @Nullable DocumentData<? extends IUniqueObject> documentData) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getWeasyPrintServiceBaseUrl() + getConvertingUrl(documentData))
                    .queryParam("presentational_hints", weasyPrintOptions.followHTMLPresentationalHints())
                    .queryParam("pdf_variant", weasyPrintOptions.pdfVariant().toWeasyPrintParameter());

            if (documentData != null && documentData.getAttachmentFiles() != null) {
                return sendMultiPartRequest(webTarget, htmlPage, documentData.getAttachmentFiles());
            } else {
                return sendConvertingRequest(webTarget, Entity.entity(htmlPage, MediaType.TEXT_HTML));
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private String getConvertingUrl(@Nullable DocumentData<? extends IUniqueObject> documentData) {
        return documentData != null && documentData.getAttachmentFiles() != null ? "/convert/html-with-attachments" : "/convert/html";
    }

    private byte[] sendMultiPartRequest(@NotNull WebTarget webTarget, @NotNull String htmlPage, @NotNull List<Path> attachmentFiles) {
        webTarget.register(MultiPartFeature.class);
        try (FormDataMultiPart multipart = new FormDataMultiPart()) {
            multipart.bodyPart(new FormDataBodyPart("html", htmlPage.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_HTML_TYPE));
            attachmentFiles.forEach(filePath -> {
                FileDataBodyPart filePart = new FileDataBodyPart("files", filePath.toFile());
                multipart.bodyPart(filePart);
            });

            return sendConvertingRequest(webTarget, Entity.entity(multipart, multipart.getMediaType()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not instantiate multi part form data", e);
        }
    }

    private byte[] sendConvertingRequest(@NotNull WebTarget webTarget, @NotNull Entity<?> requestEntity) {
        try (Response response = webTarget.request("application/pdf").post(requestEntity)) {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                InputStream inputStream = response.readEntity(InputStream.class);
                try {
                    logWeasyPrintVersionFromHeader(response);
                    return inputStream.readAllBytes();
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read response stream", e);
                }
            } else {
                String errorMessage = response.readEntity(String.class);
                throw new IllegalStateException(String.format("Not expected response from WeasyPrint Service. Status: %s, Message: [%s]", response.getStatus(), errorMessage));
            }
        }
    }

    @Override
    public WeasyPrintInfo getWeasyPrintInfo() {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getWeasyPrintServiceBaseUrl() + "/version");

            try (Response response = webTarget.request(MediaType.TEXT_PLAIN).get()) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    String responseContent = response.readEntity(String.class);

                    try {
                        return new ObjectMapper().readValue(responseContent, WeasyPrintInfo.class);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Could not parse response", e);
                    }
                } else {
                    throw new IllegalStateException("Could not get proper response from WeasyPrint Service");
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private void logWeasyPrintVersionFromHeader(Response response) {
        String actualPythonVersion = response.getHeaderString(PYTHON_VERSION_HEADER);
        String actualWeasyPrintVersion = response.getHeaderString(WEASYPRINT_VERSION_HEADER);
        String actualWeasyPrintServiceVersion = response.getHeaderString(WEASYPRINT_SERVICE_VERSION_HEADER);

        boolean hasPythonVersionChanged = hasVersionChanged(actualPythonVersion, pythonVersion);
        boolean hasWeasyPrintVersionChanged = hasVersionChanged(actualWeasyPrintVersion, weasyPrintVersion);
        boolean hasWeasyPrintServiceVersionChanged = hasVersionChanged(actualWeasyPrintServiceVersion, weasyPrintServiceVersion);

        if (hasWeasyPrintVersionChanged || hasPythonVersionChanged || hasWeasyPrintServiceVersionChanged) {
            logger.info(String.format("WeasyPrintService started from Docker image version '%s' uses WeasyPrint version '%s' and Python version '%s'", actualWeasyPrintServiceVersion, actualWeasyPrintVersion, actualPythonVersion));
        }
    }

    public boolean hasVersionChanged(String actualVersion, AtomicReference<String> version) {
        return !isEmpty(actualVersion) && !actualVersion.equals(version.getAndSet(actualVersion));
    }
}
