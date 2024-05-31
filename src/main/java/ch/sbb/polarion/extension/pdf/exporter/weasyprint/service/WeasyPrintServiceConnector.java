package ch.sbb.polarion.extension.pdf.exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model.WeasyPrintServiceVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;

public class WeasyPrintServiceConnector implements WeasyPrintConverter {

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

}
