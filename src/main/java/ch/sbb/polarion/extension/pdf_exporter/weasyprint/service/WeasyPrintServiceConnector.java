package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfA1Processor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfA4Processor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfUa2Processor;
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
        return convertToPdf(htmlPage, weasyPrintOptions, null, null);
    }

    @Override
    public byte[] convertToPdf(@NotNull String htmlPage, @NotNull WeasyPrintOptions weasyPrintOptions, @Nullable DocumentData<? extends IUniqueObject> documentData) {
        return convertToPdf(htmlPage, weasyPrintOptions, documentData, null);
    }

    @Override
    public byte[] convertToPdf(@NotNull String htmlPage, @NotNull WeasyPrintOptions weasyPrintOptions, @Nullable DocumentData<? extends IUniqueObject> documentData, @Nullable PdfGenerationLog generationLog) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getWeasyPrintServiceBaseUrl() + getConvertingUrl(documentData))
                    .queryParam("presentational_hints", weasyPrintOptions.isFollowHTMLPresentationalHints())
                    .queryParam("pdf_variant", weasyPrintOptions.getPdfVariant().toWeasyPrintParameter())
                    .queryParam("custom_metadata", weasyPrintOptions.isCustomMetadata())
                    .queryParam("scale_factor", weasyPrintOptions.getImageDensity().getScale())
                    .queryParam("full_fonts", weasyPrintOptions.isFullFonts());

            byte[] pdfBytes;
            long startTime = System.currentTimeMillis();

            if (documentData != null && documentData.getAttachmentFiles() != null) {
                pdfBytes = sendMultiPartRequest(webTarget, htmlPage, documentData.getAttachmentFiles());
                recordTiming(generationLog, "WeasyPrint conversion (with attachments)", System.currentTimeMillis() - startTime,
                        String.format("variant=%s, attachments=%d", weasyPrintOptions.getPdfVariant(), documentData.getAttachmentFiles().size()));
            } else {
                pdfBytes = sendConvertingRequest(webTarget, Entity.entity(htmlPage, MediaType.TEXT_HTML));
                recordTiming(generationLog, "WeasyPrint conversion", System.currentTimeMillis() - startTime,
                        String.format("variant=%s, html_size=%d bytes", weasyPrintOptions.getPdfVariant(), htmlPage.length()));
            }

            // Post-process PDF/A-1 documents to ensure compliance with ISO 19005-1:2005
            if (isPdfA1Variant(weasyPrintOptions.getPdfVariant())) {
                startTime = System.currentTimeMillis();
                int originalSize = pdfBytes.length;
                pdfBytes = postProcessPdfA1(pdfBytes, weasyPrintOptions.getPdfVariant());
                recordTiming(generationLog, "PDF/A-1 post-processing", System.currentTimeMillis() - startTime,
                        String.format("variant=%s, pdf_size=%d->%d bytes", weasyPrintOptions.getPdfVariant(), originalSize, pdfBytes.length));
            }

            // Post-process PDF/A-4 documents to ensure compliance with ISO 19005-4:2020
            if (isPdfA4Variant(weasyPrintOptions.getPdfVariant())) {
                startTime = System.currentTimeMillis();
                int originalSize = pdfBytes.length;
                pdfBytes = postProcessPdfA4(pdfBytes, weasyPrintOptions.getPdfVariant());
                recordTiming(generationLog, "PDF/A-4 post-processing", System.currentTimeMillis() - startTime,
                        String.format("variant=%s, pdf_size=%d->%d bytes", weasyPrintOptions.getPdfVariant(), originalSize, pdfBytes.length));
            }

            // Post-process PDF/UA-2 documents to ensure compliance with ISO 14289-2:2024
            if (weasyPrintOptions.getPdfVariant() == PdfVariant.PDF_UA_2) {
                startTime = System.currentTimeMillis();
                int originalSize = pdfBytes.length;
                pdfBytes = postProcessPdfUa2(pdfBytes);
                recordTiming(generationLog, "PDF/UA-2 post-processing", System.currentTimeMillis() - startTime,
                        String.format("pdf_size=%d->%d bytes", originalSize, pdfBytes.length));
            }

            return pdfBytes;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private void recordTiming(@Nullable PdfGenerationLog generationLog, String stageName, long durationMs, String details) {
        if (generationLog != null) {
            generationLog.recordTiming(stageName, durationMs, details);
        }
    }

    private boolean isPdfA1Variant(@NotNull PdfVariant pdfVariant) {
        return pdfVariant == PdfVariant.PDF_A_1A || pdfVariant == PdfVariant.PDF_A_1B;
    }

    private byte[] postProcessPdfA1(byte[] pdfBytes, @NotNull PdfVariant pdfVariant) {
        try {
            String conformance = switch (pdfVariant) {
                case PDF_A_1A -> "A";
                case PDF_A_1B -> "B";
                default -> null;
            };
            return PdfA1Processor.processPdfA1(pdfBytes, conformance);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/A-1 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
        }
    }

    private boolean isPdfA4Variant(@NotNull PdfVariant pdfVariant) {
        return pdfVariant == PdfVariant.PDF_A_4E || pdfVariant == PdfVariant.PDF_A_4F || pdfVariant == PdfVariant.PDF_A_4U;
    }

    private byte[] postProcessPdfA4(byte[] pdfBytes, @NotNull PdfVariant pdfVariant) {
        try {
            String conformance = switch (pdfVariant) {
                case PDF_A_4E -> "E";
                case PDF_A_4F -> "F";
                default -> null;
            };
            return PdfA4Processor.processPdfA4(pdfBytes, conformance);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/A-4 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
        }
    }

    private byte[] postProcessPdfUa2(byte[] pdfBytes) {
        try {
            return PdfUa2Processor.processPdfUa2(pdfBytes);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/UA-2 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
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
