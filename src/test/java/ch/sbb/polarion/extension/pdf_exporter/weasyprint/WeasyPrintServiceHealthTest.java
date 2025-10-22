package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.WeasyPrintHealth;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for WeasyPrintServiceConnector that require running WeasyPrint Docker service.
 * Run with: mvn test -Ptests-with-weasyprint-docker
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
                    () -> assertTrue(!health.getStatus().isEmpty(), "Health status should not be empty"),
                    () -> assertNotNull(health.getChromiumRunning(), "Chromium running flag should not be null")
            );

            // Validate metrics if present
            if (health.getMetrics() != null) {
                assertAll("Metrics validation",
                        () -> assertNotNull(health.getMetrics().getPdfGenerations(),
                                "PDF generations count should not be null"),
                        () -> assertTrue(health.getMetrics().getPdfGenerations() >= 0,
                                "PDF generations count should be non-negative"),
                        () -> assertNotNull(health.getMetrics().getFailedPdfGenerations(),
                                "Failed PDF generations count should not be null"),
                        () -> assertTrue(health.getMetrics().getFailedPdfGenerations() >= 0,
                                "Failed PDF generations count should be non-negative")
                );
            }
        } finally {
            connector.close();
        }
    }

    @Test
    void shouldParseAllHealthFieldsCorrectly() {
        // Given: Connector to running WeasyPrint Docker service
        WeasyPrintServiceConnector connector = getWeasyPrintServiceConnector();

        try {
            // When: Retrieve health data
            WeasyPrintHealth health = connector.getHealth();

            // Then: Verify all main health fields are parsed correctly
            assertAll("Main health fields",
                    () -> assertNotNull(health.getStatus(), "Status should be present"),
                    () -> assertNotNull(health.getChromiumRunning(), "Chromium running flag should be present"),
                    () -> assertTrue(health.getStatus().length() > 0, "Status should not be empty")
            );

            // Verify metrics are parsed correctly if present
            if (health.getMetrics() != null) {
                WeasyPrintHealth.Metrics metrics = health.getMetrics();
                assertAll("Metrics fields",
                        () -> assertNotNull(metrics.getPdfGenerations(), "PDF generations should be present"),
                        () -> assertNotNull(metrics.getFailedPdfGenerations(), "Failed PDF generations should be present"),
                        () -> assertTrue(metrics.getPdfGenerations() >= 0, "PDF generations should be non-negative"),
                        () -> assertTrue(metrics.getFailedPdfGenerations() >= 0, "Failed PDF generations should be non-negative")
                );

                // Verify optional metric fields
                if (metrics.getUptimeSeconds() != null) {
                    assertTrue(metrics.getUptimeSeconds() >= 0, "Uptime should be non-negative");
                }
                if (metrics.getQueueSize() != null) {
                    assertTrue(metrics.getQueueSize() >= 0, "Queue size should be non-negative");
                }
            }
        } finally {
            connector.close();
        }
    }
}
