package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.WeasyPrintHealth;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WeasyPrintServiceConnector that require running WeasyPrint Docker service.
 */
class WeasyPrintServiceHealthTest extends BaseWeasyPrintTest {

    @Test
    void shouldSuccessfullyRetrieveHealthFromWeasyPrintService() {
        // Given: Connector to running WeasyPrint Docker service
        WeasyPrintServiceConnector connector = getWeasyPrintServiceConnector();

        try {
            // When: Call getHealth() which performs HTTP request and JSON parsing
            WeasyPrintHealth health = connector.getHealth();

            // Then: Should successfully retrieve and parse health response
            assertAll("Health response validation",
                    () -> assertNotNull(health, "Health response should not be null"),
                    () -> assertNotNull(health.getStatus(), "Health status should not be null"),
                    () -> assertFalse(health.getStatus().isEmpty(), "Health status should not be empty"),
                    () -> assertNotNull(health.getChromiumRunning(), "Chromium running flag should not be null")
            );

            // Validate metrics
            WeasyPrintHealth.Metrics metrics = health.getMetrics();
            assertNotNull(metrics, "Metrics should be present");

            assertAll("Metrics validation",
                    () -> assertTrue(metrics.getUptimeSeconds() >= 0, "Uptime should be non-negative"),
                    () -> assertTrue(metrics.getQueueSize() >= 0, "Queue size should be non-negative"),
                    () -> assertNotNull(metrics.getPdfGenerations(), "PDF generations count should not be null"),
                    () -> assertTrue(metrics.getPdfGenerations() >= 0, "PDF generations should be non-negative"),
                    () -> assertNotNull(metrics.getFailedPdfGenerations(), "Failed PDF generations count should not be null"),
                    () -> assertTrue(metrics.getFailedPdfGenerations() >= 0, "Failed PDF generations should be non-negative")
            );
        } finally {
            connector.close();
        }
    }
}
