package ch.sbb.polarion.extension.pdf.exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model.WeasyPrintServiceVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.concurrent.atomic.AtomicReference;

import static com.polarion.core.util.StringUtils.isEmpty;

public class WeasyPrintServiceConnector implements WeasyPrintConverter {
    private static final Logger logger = Logger.getLogger(WeasyPrintServiceConnector.class);
    private static final String WEASYPRINT_VERSION_HEADER = "Weasyprint-Version";
    private static final String PYTHON_VERSION_HEADER = "Python-Version";
    private static final AtomicReference<String> weasyPrintVersion = new AtomicReference<>();
    private static final AtomicReference<String> pythonVersion = new AtomicReference<>();

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
                    throw new IllegalStateException("Could not get proper response from WeasyPrint Service");
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Override
    public ModuleDescriptor.Version getWeasyPrintVersion() {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getWeasyPrintServiceBaseUrl() + "/version");

            try (Response response = webTarget.request(MediaType.TEXT_PLAIN).get()) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    String responseContent = response.readEntity(String.class);

                    try {
                        WeasyPrintServiceVersion weasyPrintServiceVersion = new ObjectMapper().readValue(responseContent, WeasyPrintServiceVersion.class);
                        return ModuleDescriptor.Version.parse(weasyPrintServiceVersion.getWeasyprint());
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

    private String getWeasyPrintServiceBaseUrl() {
        return PdfExporterExtensionConfiguration.getInstance().getWeasyprintService();
    }

    private void logWeasyPrintVersionFromHeader(Response response) {
        String actualWeasyPrintVersion = response.getHeaderString(WEASYPRINT_VERSION_HEADER);
        String actualPythonVersion = response.getHeaderString(PYTHON_VERSION_HEADER);

        logWeasyPrintVersion(actualWeasyPrintVersion, weasyPrintVersion, "WeasyPrint");
        logWeasyPrintVersion(actualPythonVersion, pythonVersion, "Python");
    }

    public void logWeasyPrintVersion(String actualVersion, AtomicReference<String> version, String nameInMessage) {
        if (!isEmpty(actualVersion)
                && !actualVersion.equals(version.getAndSet(actualVersion))) {
            logger.info(String.format("Using WeasyPrint Service with %s version: %s", nameInMessage, actualVersion));
        }
    }
}
