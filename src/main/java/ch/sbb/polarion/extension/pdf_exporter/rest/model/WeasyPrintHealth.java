package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeasyPrintHealth {

    @JsonProperty("status")
    private String status;

    @JsonProperty("version")
    private String version;

    @JsonProperty("weasyprint_version")
    private String weasyprintVersion;

    @JsonProperty("chromium_running")
    private Boolean chromiumRunning;

    @JsonProperty("chromium_version")
    private String chromiumVersion;

    @JsonProperty("health_monitoring_enabled")
    private Boolean healthMonitoringEnabled;

    @JsonProperty("metrics")
    private Metrics metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metrics {

        @JsonProperty("pdf_generations")
        private Long pdfGenerations;

        @JsonProperty("failed_pdf_generations")
        private Long failedPdfGenerations;

        @JsonProperty("avg_pdf_generation_time_ms")
        private Double avgPdfGenerationTimeMs;

        @JsonProperty("total_svg_conversions")
        private Long totalSvgConversions;

        @JsonProperty("failed_svg_conversions")
        private Long failedSvgConversions;

        @JsonProperty("avg_svg_conversion_time_ms")
        private Double avgSvgConversionTimeMs;

        @JsonProperty("error_pdf_generation_rate_percent")
        private Double errorPdfGenerationRatePercent;

        @JsonProperty("error_svg_conversion_rate_percent")
        private Double errorSvgConversionRatePercent;

        @JsonProperty("total_chromium_restarts")
        private Long totalChromiumRestarts;

        @JsonProperty("last_health_check")
        private String lastHealthCheck;

        @JsonProperty("last_health_status")
        private Boolean lastHealthStatus;

        @JsonProperty("uptime_seconds")
        private Double uptimeSeconds;

        @JsonProperty("current_cpu_percent")
        private Double currentCpuPercent;

        @JsonProperty("avg_cpu_percent")
        private Double avgCpuPercent;

        @JsonProperty("total_memory_mb")
        private Double totalMemoryMb;

        @JsonProperty("available_memory_mb")
        private Double availableMemoryMb;

        @JsonProperty("current_chromium_memory_mb")
        private Double currentChromiumMemoryMb;

        @JsonProperty("avg_chromium_memory_mb")
        private Double avgChromiumMemoryMb;

        @JsonProperty("queue_size")
        private Integer queueSize;

        @JsonProperty("active_pdf_generations")
        private Integer activePdfGenerations;

        @JsonProperty("avg_queue_time_ms")
        private Double avgQueueTimeMs;

        @JsonProperty("max_concurrent_pdf_generations")
        private Integer maxConcurrentPdfGenerations;
    }
}
