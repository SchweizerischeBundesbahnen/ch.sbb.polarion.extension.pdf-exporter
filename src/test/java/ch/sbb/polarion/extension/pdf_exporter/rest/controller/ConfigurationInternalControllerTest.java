package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.exception.WeasyPrintServiceHealthException;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WeasyPrintHealth;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationInternalControllerTest {

    @Mock
    private WeasyPrintServiceConnector weasyPrintServiceConnector;

    private ConfigurationInternalController controller;

    @BeforeEach
    void setUp() {
        controller = new ConfigurationInternalController(weasyPrintServiceConnector);
    }

    @Test
    void shouldReturnHealthWhenServiceIsAvailable() {
        // Given: WeasyPrint service returns healthy status
        WeasyPrintHealth expectedHealth = WeasyPrintHealth.builder()
                .status("healthy")
                .chromiumRunning(true)
                .metrics(WeasyPrintHealth.Metrics.builder()
                        .pdfGenerations(100L)
                        .failedPdfGenerations(2L)
                        .build())
                .build();

        when(weasyPrintServiceConnector.getHealth()).thenReturn(expectedHealth);

        // When: Health endpoint is called
        WeasyPrintHealth actualHealth = controller.getWeasyPrintHealth();

        // Then: Health data is returned
        assertNotNull(actualHealth);
        assertEquals("healthy", actualHealth.getStatus());
        assertTrue(actualHealth.getChromiumRunning());
        assertEquals(100L, actualHealth.getMetrics().getPdfGenerations());
        assertEquals(2L, actualHealth.getMetrics().getFailedPdfGenerations());
    }

    @Test
    void shouldThrowWeasyPrintServiceExceptionWhenConnectorThrowsException() {
        // Given: WeasyPrint service connector throws exception
        when(weasyPrintServiceConnector.getHealth())
                .thenThrow(new IllegalStateException("Service unavailable"));

        // When & Then: Health endpoint throws WeasyPrintServiceException
        WeasyPrintServiceHealthException exception = assertThrows(
                WeasyPrintServiceHealthException.class,
                () -> controller.getWeasyPrintHealth()
        );

        assertEquals("Failed to retrieve WeasyPrint service health", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Service unavailable", exception.getCause().getMessage());
    }

    @Test
    void shouldThrowWeasyPrintServiceExceptionWhenConnectorThrowsRuntimeException() {
        // Given: WeasyPrint service connector throws runtime exception
        when(weasyPrintServiceConnector.getHealth())
                .thenThrow(new RuntimeException("Connection refused"));

        // When & Then: Health endpoint throws WeasyPrintServiceException
        WeasyPrintServiceHealthException exception = assertThrows(
                WeasyPrintServiceHealthException.class,
                () -> controller.getWeasyPrintHealth()
        );

        assertEquals("Failed to retrieve WeasyPrint service health", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Connection refused", exception.getCause().getMessage());
    }
}
