package ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Debug data for a conversion job")
public record DebugDataResponse(
        @Schema(description = "Whether debug data is available for this job")
        boolean available,

        @Schema(description = "Document title")
        String documentTitle,

        @Schema(description = "Timestamp when the debug data was created (ISO-8601)")
        String createdAt,

        @Schema(description = "Whether original HTML is available")
        boolean hasOriginalHtml,

        @Schema(description = "Whether processed HTML is available")
        boolean hasProcessedHtml,

        @Schema(description = "Whether timing report is available")
        boolean hasTimingReport,

        @Schema(description = "Message when debug data is not available")
        String message
) {
    public static DebugDataResponse notAvailable(String message) {
        return DebugDataResponse.builder()
                .available(false)
                .message(message)
                .build();
    }
}
