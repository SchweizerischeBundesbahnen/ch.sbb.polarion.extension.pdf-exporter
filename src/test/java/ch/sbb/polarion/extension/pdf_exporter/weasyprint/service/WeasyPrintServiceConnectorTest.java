package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WeasyPrintHealth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class WeasyPrintServiceConnectorTest {

    @Test
    void shouldParseHealthResponseCorrectly() throws Exception {
        // Given: Sample health response JSON from WeasyPrint service
        String healthJson = """
            {
                "status": "healthy",
                "version": "1.0.0",
                "weasyprint_version": "61.2",
                "chromium_running": true,
                "chromium_version": "131.0.6778.204",
                "health_monitoring_enabled": true,
                "metrics": {
                    "pdf_generations": 100,
                    "failed_pdf_generations": 2,
                    "avg_pdf_generation_time_ms": 1500.5,
                    "total_svg_conversions": 50,
                    "failed_svg_conversions": 1,
                    "avg_svg_conversion_time_ms": 250.3,
                    "error_pdf_generation_rate_percent": 2.0,
                    "error_svg_conversion_rate_percent": 2.0,
                    "total_chromium_restarts": 0,
                    "last_health_check": "2025-10-22T14:30:00",
                    "last_health_status": true,
                    "uptime_seconds": 3600.0,
                    "current_cpu_percent": 15.5,
                    "avg_cpu_percent": 12.3,
                    "total_memory_mb": 8192.0,
                    "available_memory_mb": 4096.0,
                    "current_chromium_memory_mb": 512.0,
                    "avg_chromium_memory_mb": 480.0,
                    "queue_size": 5,
                    "active_pdf_generations": 2,
                    "avg_queue_time_ms": 100.0,
                    "max_concurrent_pdf_generations": 4
                }
            }
            """;

        // When: Parse JSON into WeasyPrintHealth object
        ObjectMapper objectMapper = new ObjectMapper();
        WeasyPrintHealth health = objectMapper.readValue(healthJson, WeasyPrintHealth.class);

        // Then: Verify all fields are correctly parsed
        assertNotNull(health);
        assertEquals("healthy", health.getStatus());
        assertEquals("1.0.0", health.getVersion());
        assertEquals("61.2", health.getWeasyprintVersion());
        assertTrue(health.getChromiumRunning());
        assertEquals("131.0.6778.204", health.getChromiumVersion());
        assertTrue(health.getHealthMonitoringEnabled());

        // Verify metrics
        assertNotNull(health.getMetrics());
        assertEquals(100L, health.getMetrics().getPdfGenerations());
        assertEquals(2L, health.getMetrics().getFailedPdfGenerations());
        assertEquals(1500.5, health.getMetrics().getAvgPdfGenerationTimeMs());
        assertEquals(50L, health.getMetrics().getTotalSvgConversions());
        assertEquals(1L, health.getMetrics().getFailedSvgConversions());
        assertEquals(250.3, health.getMetrics().getAvgSvgConversionTimeMs());
        assertEquals(2.0, health.getMetrics().getErrorPdfGenerationRatePercent());
        assertEquals(2.0, health.getMetrics().getErrorSvgConversionRatePercent());
        assertEquals(0L, health.getMetrics().getTotalChromiumRestarts());
        assertEquals("2025-10-22T14:30:00", health.getMetrics().getLastHealthCheck());
        assertTrue(health.getMetrics().getLastHealthStatus());
        assertEquals(3600.0, health.getMetrics().getUptimeSeconds());
        assertEquals(15.5, health.getMetrics().getCurrentCpuPercent());
        assertEquals(12.3, health.getMetrics().getAvgCpuPercent());
        assertEquals(8192.0, health.getMetrics().getTotalMemoryMb());
        assertEquals(4096.0, health.getMetrics().getAvailableMemoryMb());
        assertEquals(512.0, health.getMetrics().getCurrentChromiumMemoryMb());
        assertEquals(480.0, health.getMetrics().getAvgChromiumMemoryMb());
        assertEquals(5, health.getMetrics().getQueueSize());
        assertEquals(2, health.getMetrics().getActivePdfGenerations());
        assertEquals(100.0, health.getMetrics().getAvgQueueTimeMs());
        assertEquals(4, health.getMetrics().getMaxConcurrentPdfGenerations());
    }

    @Test
    void shouldParseMinimalHealthResponse() throws Exception {
        // Given: Minimal health response without optional fields
        String healthJson = """
            {
                "status": "unhealthy",
                "chromium_running": false
            }
            """;

        // When: Parse JSON into WeasyPrintHealth object
        ObjectMapper objectMapper = new ObjectMapper();
        WeasyPrintHealth health = objectMapper.readValue(healthJson, WeasyPrintHealth.class);

        // Then: Verify mandatory fields are parsed, optional fields are null
        assertNotNull(health);
        assertEquals("unhealthy", health.getStatus());
        assertEquals(false, health.getChromiumRunning());
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        // Given: Health response with unknown fields
        String healthJson = """
            {
                "status": "healthy",
                "unknown_field": "should be ignored",
                "another_unknown": 123
            }
            """;

        // When: Parse JSON into WeasyPrintHealth object
        ObjectMapper objectMapper = new ObjectMapper();
        WeasyPrintHealth health = objectMapper.readValue(healthJson, WeasyPrintHealth.class);

        // Then: Parsing should succeed, unknown fields ignored
        assertNotNull(health);
        assertEquals("healthy", health.getStatus());
    }

    @Test
    void shouldHandleNullMetrics() throws Exception {
        // Given: Health response without metrics
        String healthJson = """
            {
                "status": "degraded",
                "chromium_running": true
            }
            """;

        // When: Parse JSON into WeasyPrintHealth object
        ObjectMapper objectMapper = new ObjectMapper();
        WeasyPrintHealth health = objectMapper.readValue(healthJson, WeasyPrintHealth.class);

        // Then: Metrics should be null
        assertNotNull(health);
        assertEquals("degraded", health.getStatus());
    }

    @Test
    void shouldConstructConnectorWithBaseUrl() {
        // Given: Custom base URL
        String customUrl = "http://custom-host:9090";

        // When: Create connector with custom URL
        WeasyPrintServiceConnector connector = new WeasyPrintServiceConnector(customUrl);

        // Then: Base URL should be set correctly
        assertEquals(customUrl, connector.getWeasyPrintServiceBaseUrl());
    }

    @Test
    void shouldDetectVersionChange() {
        // Given: Connector instance
        WeasyPrintServiceConnector connector = new WeasyPrintServiceConnector("http://localhost:9080");
        java.util.concurrent.atomic.AtomicReference<String> versionRef = new java.util.concurrent.atomic.AtomicReference<>();

        // When: First version set
        boolean firstChange = connector.hasVersionChanged("1.0.0", versionRef);

        // Then: Should detect change from null to 1.0.0
        assertTrue(firstChange);
        assertEquals("1.0.0", versionRef.get());

        // When: Same version again
        boolean noChange = connector.hasVersionChanged("1.0.0", versionRef);

        // Then: Should not detect change
        assertFalse(noChange);
        assertEquals("1.0.0", versionRef.get());

        // When: Different version
        boolean secondChange = connector.hasVersionChanged("1.1.0", versionRef);

        // Then: Should detect change
        assertTrue(secondChange);
        assertEquals("1.1.0", versionRef.get());
    }
}
