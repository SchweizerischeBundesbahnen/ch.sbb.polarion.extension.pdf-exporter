package ch.sbb.polarion.extension.pdf_exporter.pandoc;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
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

public class PandocServiceConnector {

    private static final Logger logger = Logger.getLogger(WeasyPrintServiceConnector.class);

    private static final String PYTHON_VERSION_HEADER = "Python-Version";
    private static final String PANDOC_VERSION_HEADER = "Pandoc-Version";
    private static final String PANDOC_SERVICE_VERSION_HEADER = "Pandoc-Service-Version";

    private static final AtomicReference<String> pythonVersion = new AtomicReference<>();
    private static final AtomicReference<String> pandocVersion = new AtomicReference<>();
    private static final AtomicReference<String> pandocServiceVersion = new AtomicReference<>();

    @Getter
    private final @NotNull String pandocServiceBaseUrl;

    public PandocServiceConnector() {
        this(PdfExporterExtensionConfiguration.getInstance().getPandocService());
    }

    public PandocServiceConnector(@NotNull String pandocServiceBaseUrl) {
        this.pandocServiceBaseUrl = pandocServiceBaseUrl;
    }

    public byte[] convertToDocx(String htmlPage) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(getPandocServiceBaseUrl() + "/convert/html/to/docx");

            try (Response response = webTarget.request("application/vnd.openxmlformats-officedocument.wordprocessingml.document").post(Entity.entity(htmlPage, MediaType.TEXT_HTML))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    InputStream inputStream = response.readEntity(InputStream.class);
                    try {
                        logPandocVersionFromHeader(response);
                        return inputStream.readAllBytes();
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not read response stream", e);
                    }
                } else {
                    String errorMessage = response.readEntity(String.class);
                    throw new IllegalStateException(String.format("Not expected response from Pandoc Service. Status: %s, Message: [%s]", response.getStatus(), errorMessage));
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private void logPandocVersionFromHeader(Response response) {
        String actualPythonVersion = response.getHeaderString(PYTHON_VERSION_HEADER);
        String actualPandocVersion = response.getHeaderString(PANDOC_VERSION_HEADER);
        String actualPandocServiceVersion = response.getHeaderString(PANDOC_SERVICE_VERSION_HEADER);

        boolean hasPythonVersionChanged = hasVersionChanged(actualPythonVersion, pythonVersion);
        boolean hasPandocVersionChanged = hasVersionChanged(actualPandocVersion, pandocVersion);
        boolean hasPandocServiceVersionChanged = hasVersionChanged(actualPandocServiceVersion, pandocServiceVersion);

        if (hasPandocVersionChanged || hasPythonVersionChanged || hasPandocServiceVersionChanged) {
            logger.info(String.format("PandocService started from Docker image version '%s' uses Pandoc version '%s' and Python version '%s'", actualPandocServiceVersion, actualPandocVersion, actualPythonVersion));
        }
    }

    public boolean hasVersionChanged(String actualVersion, AtomicReference<String> version) {
        return !isEmpty(actualVersion) && !actualVersion.equals(version.getAndSet(actualVersion));
    }
}
