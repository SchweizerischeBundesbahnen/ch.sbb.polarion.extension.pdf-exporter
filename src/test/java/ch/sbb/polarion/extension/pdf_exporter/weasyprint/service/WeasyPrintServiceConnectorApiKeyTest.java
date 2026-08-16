package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeasyPrintServiceConnectorApiKeyTest {

    private static final String API_KEY = "s3cr3t";

    private static WeasyPrintServiceConnector connector() {
        return new WeasyPrintServiceConnector("http://localhost:9080", new ApiKeyProvider());
    }

    @Test
    void shouldSendConfiguredKeyAsApiKeyHeader() {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Invocation.Builder builderWithHeader = mock(Invocation.Builder.class);
        when(webTarget.request("application/pdf")).thenReturn(builder);
        when(builder.header("X-API-Key", API_KEY)).thenReturn(builderWithHeader);

        assertThat(connector().requestWithApiKey(webTarget, API_KEY)).isSameAs(builderWithHeader);
    }

    @Test
    void shouldSendNoHeaderWhenNoKeyConfigured() {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request("application/pdf")).thenReturn(builder);

        assertThat(connector().requestWithApiKey(webTarget, null)).isSameAs(builder);
        verify(builder, never()).header(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldTellTheTwoUnauthorizedCausesApart() {
        assertThat(WeasyPrintServiceConnector.unauthorizedMessage(true))
                .contains("rejected the configured API key")
                .contains(PdfExporterExtensionConfiguration.WEASYPRINT_API_KEY_SECRET);

        assertThat(WeasyPrintServiceConnector.unauthorizedMessage(false))
                .contains("requires an API key, none is configured")
                .contains(PdfExporterExtensionConfiguration.WEASYPRINT_API_KEY_SECRET);
    }
}
