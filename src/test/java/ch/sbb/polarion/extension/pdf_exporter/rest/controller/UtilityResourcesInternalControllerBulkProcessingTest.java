package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.model.BulkProcessingServiceStatus;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class UtilityResourcesInternalControllerBulkProcessingTest {

    private MockedStatic<ClientBuilder> clientBuilderMockedStatic;
    private Client client;
    private Invocation.Builder invocationBuilder;
    private UtilityResourcesInternalController controller;

    @BeforeEach
    void setUp() {
        client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        invocationBuilder = mock(Invocation.Builder.class);

        clientBuilderMockedStatic = mockStatic(ClientBuilder.class);
        clientBuilderMockedStatic.when(ClientBuilder::newClient).thenReturn(client);
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.request(MediaType.APPLICATION_JSON)).thenReturn(invocationBuilder);

        PdfExporterExtensionConfiguration configMock = PdfExporterExtensionConfiguration.getInstance();
        when(configMock.getBulkProcessingService()).thenReturn("http://localhost:9070");

        controller = new UtilityResourcesInternalController(mock(PdfExporterPolarionService.class));
    }

    @AfterEach
    void tearDown() {
        clientBuilderMockedStatic.close();
    }

    @Test
    void shouldReturnAvailableWhenHealthCheckSucceeds() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(invocationBuilder.get()).thenReturn(response);

        BulkProcessingServiceStatus status = controller.getBulkProcessingServiceStatus();

        assertThat(status.isAvailable()).isTrue();
    }

    @Test
    void shouldReturnUnavailableWhenHealthCheckReturnsNon200() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(503);
        when(invocationBuilder.get()).thenReturn(response);

        BulkProcessingServiceStatus status = controller.getBulkProcessingServiceStatus();

        assertThat(status.isAvailable()).isFalse();
    }

    @Test
    void shouldReturnUnavailableWhenConnectionFails() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException("Connection refused"));

        BulkProcessingServiceStatus status = controller.getBulkProcessingServiceStatus();

        assertThat(status.isAvailable()).isFalse();
    }
}
