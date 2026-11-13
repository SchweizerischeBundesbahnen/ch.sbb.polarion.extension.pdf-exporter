package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhookConfig;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhooksModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
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
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
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
    private PdfExporterExtensionConfiguration configuration;

    @BeforeEach
    void setUp() {
        // The extension has already mocked getInstance() by now, and that mock is the one the converter reads.
        configuration = PdfExporterExtensionConfiguration.getInstance();
        lenient().when(configuration.getWebhooksEnabled()).thenReturn(true);

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
    void appliesEveryWebhookOfTheConfigurationInTurn() {
        Response first = mockResponse(200);
        when(first.readEntity(InputStream.class)).thenReturn(bodyOf("<html>first</html>"));
        Response second = mockResponse(200);
        when(second.readEntity(InputStream.class)).thenReturn(bodyOf("<html>second</html>"));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(first, second);

        WebhooksModel model = WebhooksModel.builder()
                .webhookConfigs(List.of(new WebhookConfig(URL, null, null, false), new WebhookConfig(URL, null, null, false)))
                .build();
        try (MockedConstruction<WebhooksSettings> settings = mockConstruction(WebhooksSettings.class,
                (mock, context) -> when(mock.load(anyString(), any(SettingId.class))).thenReturn(model))) {

            String result = pdfConverter.applyWebhooks(exportParams, HTML);

            // The HTML of each webhook is what the next one is given, so the last one decides the result.
            assertThat(result).isEqualTo("<html>second</html>");
            assertThat(settings.constructed()).hasSize(1);
        }
    }

    @Test
    void skipsWebhooksWhenTheFeatureIsOff() {
        when(configuration.getWebhooksEnabled()).thenReturn(false);

        assertThat(pdfConverter.applyWebhooks(exportParams, HTML)).isEqualTo(HTML);
        verifyNoInteractions(invocationBuilder);
    }

    @Test
    void skipsWebhooksWhenTheExportSelectedNone() {
        ExportParams withoutWebhooks = ExportParams.builder().projectId("project").build();

        assertThat(pdfConverter.applyWebhooks(withoutWebhooks, HTML)).isEqualTo(HTML);
        verifyNoInteractions(invocationBuilder);
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
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);

        String result = pdfConverter.applyWebhook(new WebhookConfig(URL, null, null, false), 1, exportParams, HTML);

        assertThat(result).isEqualTo(HTML);
        // An export which carries on has no use for the body, so it is never pulled over the wire.
        verify(response, never()).readEntity(InputStream.class);
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
        when(response.readEntity(InputStream.class)).thenReturn(bodyOf("  Document is not approved\n"));
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
        when(response.readEntity(InputStream.class)).thenReturn(bodyOf("Field <title> & \"status\" are not set"));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Field <title> & \"status\" are not set");
    }

    @Test
    void failsExportWithTheFallbackWhenTheWebhookExplainsNothing() {
        Response response = mockResponse(500);
        when(response.readEntity(InputStream.class)).thenReturn(bodyOf(""));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which returned HTTP 500")
                .message().doesNotContain("secret");
    }

    @Test
    void fallsBackToTheStatusWhenTheResponseBodyCannotBeRead() {
        Response response = mockResponse(502);
        when(response.readEntity(InputStream.class)).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream closed");
            }
        });
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which returned HTTP 502");
    }

    @Test
    void truncatesLongResponseBody() {
        Response response = mockResponse(400);
        when(response.readEntity(InputStream.class)).thenReturn(bodyOf("x".repeat(600)));
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("x".repeat(500) + "...");
    }

    @Test
    void failsExportOnCallFailureNamingTheKindOfTheFailure() {
        // Jersey wraps what went wrong; the innermost cause is the one that names it.
        when(invocationBuilder.post(any(Entity.class)))
                .thenThrow(new ProcessingException(new ConnectException("Connection refused")));
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which could not be called (ConnectException)");
    }

    @Test
    void keepsTheWebhookUrlOutOfTheMessageOfACallFailure() {
        // A malformed URL fails while the target is built, and that exception carries the URL it was given.
        when(invocationBuilder.post(any(Entity.class)))
                .thenThrow(new IllegalArgumentException("Illegal character in query at index 32: " + URL));
        WebhookConfig webhookConfig = new WebhookConfig(URL, null, null, true);

        assertThatThrownBy(() -> pdfConverter.applyWebhook(webhookConfig, 1, exportParams, HTML))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessage("Export stopped by webhook 1 of configuration 'Validation', which could not be called (IllegalArgumentException)")
                .message().doesNotContain("secret", "example.com");
    }

    private static Response mockResponse(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }

    private static InputStream bodyOf(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }
}
