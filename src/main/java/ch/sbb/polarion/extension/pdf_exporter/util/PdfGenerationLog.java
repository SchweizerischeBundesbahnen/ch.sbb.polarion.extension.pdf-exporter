package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.ExecutionProfiler;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF generation profiler that extends the generic ExecutionProfiler
 * with PDF-specific metrics and category breakdown.
 */
public class PdfGenerationLog extends ExecutionProfiler {

    private long htmlSizeBytes;
    private long pdfSizeBytes;
    private int pageCount;
    private String pdfVariant;

    public void setHtmlSize(long sizeBytes) {
        this.htmlSizeBytes = sizeBytes;
    }

    public void setPdfMetrics(long sizeBytes, int pages, String variant) {
        this.pdfSizeBytes = sizeBytes;
        this.pageCount = pages;
        this.pdfVariant = variant;
    }

    /**
     * Generates a PDF-specific timing report with additional metrics.
     *
     * @param documentTitle the document title
     * @return formatted timing report
     */
    public String generateTimingReport(String documentTitle) {
        StringBuilder report = new StringBuilder();

        appendHeader(report, documentTitle);
        appendSummaryStatistics(report);
        appendTimingBreakdown(report);
        appendCategoryBreakdown(report);
        appendSlowestStages(report);
        appendTimeline(report);
        appendDetailedLog(report);

        return report.toString();
    }

    @Override
    protected void appendHeader(StringBuilder report, @Nullable String title) {
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("PDF GENERATION TIMING REPORT").append(System.lineSeparator());
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("Document: ").append(title != null ? title : "Unknown").append(System.lineSeparator());
        report.append("Generated: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)).append(System.lineSeparator());
        report.append("Total Duration: ").append(formatDuration(getTotalDurationMs())).append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    private void appendSummaryStatistics(StringBuilder report) {
        report.append("SUMMARY STATISTICS:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        if (htmlSizeBytes > 0) {
            report.append(String.format("  HTML Size:      %s%n", formatSize(htmlSizeBytes)));
        }
        if (pdfSizeBytes > 0) {
            report.append(String.format("  PDF Size:       %s%n", formatSize(pdfSizeBytes)));
        }
        if (pageCount > 0) {
            report.append(String.format("  Page Count:     %d%n", pageCount));
        }
        if (pdfVariant != null) {
            report.append(String.format("  PDF Variant:    %s%n", pdfVariant));
        }
        if (pageCount > 0 && getTotalDurationMs() > 0) {
            report.append(String.format("  Avg per Page:   %s%n", formatDuration(getTotalDurationMs() / pageCount)));
        }
        if (htmlSizeBytes > 0 && getTotalDurationMs() > 0) {
            double kbPerSec = (htmlSizeBytes / 1024.0) / (getTotalDurationMs() / 1000.0);
            report.append(String.format("  Throughput:     %.1f KB/s (HTML)%n", kbPerSec));
        }

        report.append(System.lineSeparator());
    }

    private static final String OTHER = "Other";

    /** The categories of the stages, in the order they are tried: a stage whose name matches two takes the first. */
    private static final Map<String, List<String>> CATEGORY_PATTERNS = categoryPatterns();

    private static Map<String, List<String>> categoryPatterns() {
        Map<String, List<String>> patterns = new LinkedHashMap<>();
        patterns.put("WeasyPrint Conversion", List.of("weasyprint", "conversion"));
        patterns.put("PDF Post-processing", List.of("pdf/a", "pdf/ua", "post-process", "merge"));
        patterns.put("Cover Page", List.of("cover", "title"));
        patterns.put("HTML Processing", List.of("html", "css", "header", "footer", "link", "webhook", "content", "meta"));
        return Collections.unmodifiableMap(patterns);
    }

    private void appendCategoryBreakdown(StringBuilder report) {
        if (getTimingEntries().isEmpty()) {
            return;
        }

        Map<String, Long> categories = timeByCategory();

        report.append("TIME BY CATEGORY:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        categories.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    double percent = getTotalDurationMs() > 0 ? (e.getValue() * 100.0 / getTotalDurationMs()) : 0;
                    String bar = createBar(percent);
                    report.append(String.format("  %-25s %7d ms %6.1f%%  %s%n", e.getKey(), e.getValue(), percent, bar));
                });

        report.append(System.lineSeparator());
    }

    /**
     * The time of the stages by category, each stage counted for its own time: its duration less that of the stages
     * nested in it, which count for theirs. Summing the durations themselves would count a nested stage twice, in its
     * own category and in that of the stage around it.
     */
    Map<String, Long> timeByCategory() {
        Map<String, Long> categories = new LinkedHashMap<>();
        CATEGORY_PATTERNS.keySet().forEach(category -> categories.put(category, 0L));
        categories.put(OTHER, 0L);

        // A stage is recorded when its timer stops, so after the stages nested in it: their time waits under its depth
        Map<Integer, Long> nestedTimeByDepth = new HashMap<>();
        for (TimingEntry entry : getTimingEntries()) {
            Long nested = nestedTimeByDepth.remove(entry.depth() + 1);
            long ownTime = Math.max(0, entry.durationMs() - (nested != null ? nested : 0));
            nestedTimeByDepth.merge(entry.depth(), entry.durationMs(), Long::sum);
            categories.merge(categorize(entry.stageName().toLowerCase(Locale.ROOT)), ownTime, Long::sum);
        }
        return categories;
    }

    private static String categorize(String stageName) {
        return CATEGORY_PATTERNS.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(stageName::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(OTHER);
    }
}
