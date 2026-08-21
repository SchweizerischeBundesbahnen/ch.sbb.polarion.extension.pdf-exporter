package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeasyPrintServiceConnectorApiKeyTest {

    private static final String API_KEY = "s3cr3t";

    private static WeasyPrintServiceConnector connector() {
        return new WeasyPrintServiceConnector("https://weasyprint.intranet:9080", new ApiKeyProvider());
    }

    private static WeasyPrintServiceConnector plainHttpConnector() {
        return new WeasyPrintServiceConnector("http://weasyprint.intranet:9080", new ApiKeyProvider());
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
    void shouldReadPdfFromSuccessfulResponse() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(InputStream.class)).thenReturn(new ByteArrayInputStream("PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(connector().readConversionResponse(response, false)).isEqualTo("PDF".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReportRejectedKeyOnUnauthorized() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(401);
        WeasyPrintServiceConnector connector = connector();

        assertThatThrownBy(() -> connector.readConversionResponse(response, true))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("rejected the configured API key");
    }

    @Test
    void shouldReportMissingKeyOnUnauthorized() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(401);
        WeasyPrintServiceConnector connector = connector();

        assertThatThrownBy(() -> connector.readConversionResponse(response, false))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("requires an API key, none is configured");
    }

    @Test
    void shouldReportOtherStatusWithServiceMessage() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenReturn("boom");
        WeasyPrintServiceConnector connector = connector();

        assertThatThrownBy(() -> connector.readConversionResponse(response, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Status: 500")
                .hasMessageContaining("boom");
    }

    @Test
    void shouldRefuseToSendTheKeyOverPlainHttp() {
        // a key is a reusable credential: on plain http anyone on the path keeps it
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request("application/pdf")).thenReturn(builder);
        WeasyPrintServiceConnector connector = plainHttpConnector();

        assertThatThrownBy(() -> connector.requestWithApiKey(webTarget, API_KEY))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining("not sent over plain http")
                .hasMessageContaining(PdfExporterExtensionConfiguration.WEASYPRINT_API_KEY_SECRET)
                .hasMessageNotContaining(API_KEY);
        verify(builder, never()).header(ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    void shouldKeepWorkingOverPlainHttpWhenNoKeyIsConfigured() {
        // the transport rule guards a credential; where there is none, nothing changes
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request("application/pdf")).thenReturn(builder);

        assertThat(plainHttpConnector().requestWithApiKey(webTarget, null)).isSameAs(builder);
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
