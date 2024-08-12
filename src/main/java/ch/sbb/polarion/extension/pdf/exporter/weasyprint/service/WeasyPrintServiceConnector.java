package ch.sbb.polarion.extension.pdf.exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model.WeasyPrintInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
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
        this(PdfExporterExtensionConfiguration.getInstance().getWeasyprintService());
    }

    public WeasyPrintServiceConnector(@NotNull String weasyPrintServiceBaseUrl) {
        this.weasyPrintServiceBaseUrl = weasyPrintServiceBaseUrl;
    }

    @Override
    public byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getWeasyPrintServiceBaseUrl() + "/convert/html")
                    .queryParam("presentational_hints", weasyPrintOptions.followHTMLPresentationalHints());

            String weasyprintPdfVariant = PdfExporterExtensionConfiguration.getInstance().getWeasyprintPdfVariant();
            if (weasyprintPdfVariant != null) {
                webTarget = webTarget.queryParam("pdf_variant", weasyprintPdfVariant);
            }

            try (Response response = webTarget.request("application/pdf").post(Entity.entity(htmlPage, MediaType.TEXT_HTML))) {
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
        } finally {
            if (client != null) {
                client.close();
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
