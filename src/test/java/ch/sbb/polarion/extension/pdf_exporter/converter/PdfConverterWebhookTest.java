package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhookConfig;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfConverterWebhookTest {

    private static final String URL = "https://example.com/webhook?token=secret";
    private static final String HTML = "<html>initial</html>";

    @Mock
    private Client client;
    @Mock
    private WebTarget webTarget;
    @Mock
    private Invocation.Builder invocationBuilder;

    private MockedStatic<ClientBuilder> clientBuilderMockedStatic;
    private PdfConverter pdfConverter;
    private ExportParams exportParams;

    @BeforeEach
    void setUp() {
        clientBuilderMockedStatic = mockStatic(ClientBuilder.class);
        clientBuilderMockedStatic.when(ClientBuilder::newClient).thenReturn(client);
        lenient().when(client.target(anyString())).thenReturn(webTarget);
        lenient().when(webTarget.register(any(Class.class))).thenReturn(webTarget);
        lenient().when(webTarget.request(anyString())).thenReturn(invocationBuilder);

        pdfConverter = new PdfConverter(null, null, null, null, null, null, null, null, null, null);
        exportParams = ExportParams.builder().projectId("project").webhooks("Validation").build();
    }

    @AfterEach
    void tearDown() {
        clientBuilderMockedStatic.close();
    }

    @Test
    void returnsWebhookResponseOnSuccess() {
        Response response = mockResponse(200);
        when(response.readEntity(InputStream.class)).thenReturn(new ByteArrayInputStream("<html>changed</html>".getBytes(StandardCharsets.UTF_8)));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);

        String result = pdfConverter.applyWebhook(new WebhookConfig(URL, null, null, true), 1, exportParams, HTML);

        assertThat(result).isEqualTo("<html>changed</html>");
    }

    @Test
    void keepsHtmlOnErrorStatusWhenNotConfiguredToFail() {
        Response response = mockResponse(422);
        lenient().when(response.readEntity(String.class)).thenReturn("Document is not approved");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);

        String result = pdfConverter.applyWebhook(new WebhookConfig(URL, null, null, false), 1, exportParams, HTML);

        assertThat(result).isEqualTo(HTML);
    }

    @Test
    void keepsHtmlOnCallFailureWhenNotConfiguredToFail() {
        when(invocationBuilder.post(any(Entity.class))).thenThrow(new ProcessingException("Connection refused"));

        String result = pdfConverter.applyWebhook(new WebhookConfig(URL, null, null, false), 1, exportParams, HTML);

        assertThat(result).isEqualTo(HTML);
    }

    @Test
    void failsExportWithTheMessageOfTheWebhookItself() {
        Response response = mockResponse(422);
        when(response.readEntity(String.class)).thenReturn("  Document is not approved\n");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        // What the webhook wrote, and nothing of this extension's around it: the webhook knows why it refused.
        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 2, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Document is not approved");
    }

    @Test
    void passesTheResponseBodyOnUnescaped() {
        // Nothing renders it as HTML, so escaping here would show the escapes themselves to the user.
        Response response = mockResponse(422);
        when(response.readEntity(String.class)).thenReturn("Field <title> & \"status\" are not set");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Field <title> & \"status\" are not set");
    }

    @Test
    void failsExportWithTheFallbackWhenTheWebhookExplainsNothing() {
        Response response = mockResponse(500);
        when(response.readEntity(String.class)).thenReturn("");
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which returned HTTP 500")
                .message().doesNotContain("secret");
    }

    @Test
    void truncatesLongResponseBody() {
        Response response = mockResponse(400);
        when(response.readEntity(String.class)).thenReturn("x".repeat(600));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("x".repeat(500) + "...");
    }

    @Test
    void failsExportOnCallFailure() {
        when(invocationBuilder.post(any(Entity.class))).thenThrow(new ProcessingException("Connection refused"));
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which could not be called: Connection refused");
    }

    private static Response mockResponse(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }
}
