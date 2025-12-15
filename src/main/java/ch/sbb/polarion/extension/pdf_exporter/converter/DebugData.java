package ch.sbb.polarion.extension.pdf_exporter.converter;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Builder
public record DebugData(
        @Nullable String originalHtml,
        @Nullable String processedHtml,
        @Nullable String timingReport,
        String user,
        Instant createdAt,
        @Nullable String documentTitle
) {
    public boolean hasContent() {
        return (originalHtml != null && !originalHtml.isEmpty())
                || (processedHtml != null && !processedHtml.isEmpty())
                || (timingReport != null && !timingReport.isEmpty());
    }
}
