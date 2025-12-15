package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class PdfGenerationLog {

    private static final int BAR_WIDTH = 40;
    private static final char BAR_CHAR = '█';
    private static final char BAR_EMPTY = '░';

    private final StringBuilder builder = new StringBuilder();
    private final List<TimingEntry> timingEntries = new ArrayList<>();
    private final Stack<String> timerStack = new Stack<>();
    private final long startTime;

    @Getter
    private long totalDurationMs;

    private long htmlSizeBytes;
    private long pdfSizeBytes;
    private int pageCount;
    private String pdfVariant;

    public PdfGenerationLog() {
        this.startTime = System.currentTimeMillis();
    }

    public void log(String message) {
        builder.append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
                .append(" ")
                .append(message)
                .append(System.lineSeparator());
    }

    public void recordTiming(@NotNull String stageName, long durationMs) {
        recordTiming(stageName, durationMs, null);
    }

    public void recordTiming(@NotNull String stageName, long durationMs, @Nullable String details) {
        int depth = timerStack.size();
        String parentStage = timerStack.isEmpty() ? null : timerStack.peek();
        timingEntries.add(new TimingEntry(stageName, durationMs, details, depth, parentStage));

        String indent = "  ".repeat(depth);
        String message = details != null
                ? String.format("%s%s completed in %d ms (%s)", indent, stageName, durationMs, details)
                : String.format("%s%s completed in %d ms", indent, stageName, durationMs);
        log(message);
    }

    public void setHtmlSize(long sizeBytes) {
        this.htmlSizeBytes = sizeBytes;
    }

    public void setPdfMetrics(long sizeBytes, int pages, String variant) {
        this.pdfSizeBytes = sizeBytes;
        this.pageCount = pages;
        this.pdfVariant = variant;
    }

    public void finish() {
        this.totalDurationMs = System.currentTimeMillis() - startTime;
    }

    public String getLog() {
        return builder.toString();
    }

    public List<TimingEntry> getTimingEntries() {
        return new ArrayList<>(timingEntries);
    }

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

    private void appendHeader(StringBuilder report, String documentTitle) {
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("PDF GENERATION TIMING REPORT").append(System.lineSeparator());
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("Document: ").append(documentTitle != null ? documentTitle : "Unknown").append(System.lineSeparator());
        report.append("Generated: ").append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)).append(System.lineSeparator());
        report.append("Total Duration: ").append(formatDuration(totalDurationMs)).append(System.lineSeparator());
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
        if (pageCount > 0 && totalDurationMs > 0) {
            report.append(String.format("  Avg per Page:   %s%n", formatDuration(totalDurationMs / pageCount)));
        }
        if (htmlSizeBytes > 0 && totalDurationMs > 0) {
            double kbPerSec = (htmlSizeBytes / 1024.0) / (totalDurationMs / 1000.0);
            report.append(String.format("  Throughput:     %.1f KB/s (HTML)%n", kbPerSec));
        }

        report.append(System.lineSeparator());
    }

    private void appendTimingBreakdown(StringBuilder report) {
        report.append("TIMING BREAKDOWN:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(String.format("%-40s %10s %7s  %s%n", "Stage", "Duration", "Percent", "Visual"));
        report.append("-".repeat(80)).append(System.lineSeparator());

        List<TimingEntry> orderedEntries = getOrderedEntries();

        long accountedTime = 0;
        for (TimingEntry entry : orderedEntries) {
            double percent = totalDurationMs > 0 ? (entry.durationMs() * 100.0 / totalDurationMs) : 0;
            String indent = "  ".repeat(entry.depth());
            String stageName = truncate(indent + entry.stageName(), 40);
            String bar = createBar(percent);

            report.append(String.format("%-40s %7d ms %6.1f%%  %s%n", stageName, entry.durationMs(), percent, bar));
            if (entry.details() != null) {
                report.append(String.format("  %s└─ %s%n", "  ".repeat(entry.depth()), entry.details()));
            }
            if (entry.depth() == 0) {
                accountedTime += entry.durationMs();
            }
        }

        long unaccountedTime = totalDurationMs - accountedTime;
        if (unaccountedTime > 0 && totalDurationMs > 0) {
            double percent = unaccountedTime * 100.0 / totalDurationMs;
            String bar = createBar(percent);
            report.append(String.format("%-40s %7d ms %6.1f%%  %s%n", "(other/overhead)", unaccountedTime, percent, bar));
        }

        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(String.format("%-40s %7d ms %6.1f%%%n", "TOTAL", totalDurationMs, 100.0));
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    private List<TimingEntry> getOrderedEntries() {
        List<TimingEntry> result = new ArrayList<>();
        Map<String, List<TimingEntry>> childrenMap = new LinkedHashMap<>();

        for (TimingEntry entry : timingEntries) {
            String parent = entry.parentStage();
            childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(entry);
        }

        List<TimingEntry> rootEntries = childrenMap.getOrDefault(null, new ArrayList<>());
        for (TimingEntry root : rootEntries) {
            addEntryWithChildren(root, childrenMap, result);
        }

        return result;
    }

    private void addEntryWithChildren(TimingEntry entry, Map<String, List<TimingEntry>> childrenMap, List<TimingEntry> result) {
        result.add(entry);
        List<TimingEntry> children = childrenMap.get(entry.stageName());
        if (children != null) {
            for (TimingEntry child : children) {
                addEntryWithChildren(child, childrenMap, result);
            }
        }
    }

    private void appendCategoryBreakdown(StringBuilder report) {
        if (timingEntries.isEmpty()) {
            return;
        }

        Map<String, Long> categories = new LinkedHashMap<>();
        categories.put("HTML Processing", 0L);
        categories.put("WeasyPrint Conversion", 0L);
        categories.put("PDF Post-processing", 0L);
        categories.put("Cover Page", 0L);
        categories.put("Other", 0L);

        for (TimingEntry entry : timingEntries) {
            String name = entry.stageName().toLowerCase();
            if (name.contains("weasyprint") || name.contains("conversion")) {
                categories.merge("WeasyPrint Conversion", entry.durationMs(), Long::sum);
            } else if (name.contains("pdf/a") || name.contains("pdf/ua") || name.contains("post-process") || name.contains("merge")) {
                categories.merge("PDF Post-processing", entry.durationMs(), Long::sum);
            } else if (name.contains("cover") || name.contains("title")) {
                categories.merge("Cover Page", entry.durationMs(), Long::sum);
            } else if (name.contains("html") || name.contains("css") || name.contains("header") || name.contains("footer")
                    || name.contains("link") || name.contains("webhook") || name.contains("content") || name.contains("meta")) {
                categories.merge("HTML Processing", entry.durationMs(), Long::sum);
            } else {
                categories.merge("Other", entry.durationMs(), Long::sum);
            }
        }

        report.append("TIME BY CATEGORY:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        categories.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    double percent = totalDurationMs > 0 ? (e.getValue() * 100.0 / totalDurationMs) : 0;
                    String bar = createBar(percent);
                    report.append(String.format("  %-25s %7d ms %6.1f%%  %s%n", e.getKey(), e.getValue(), percent, bar));
                });

        report.append(System.lineSeparator());
    }

    private void appendSlowestStages(StringBuilder report) {
        if (timingEntries.isEmpty()) {
            return;
        }

        report.append("SLOWEST STAGES (potential bottlenecks):").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        timingEntries.stream()
                .sorted(Comparator.comparingLong(TimingEntry::durationMs).reversed())
                .limit(5)
                .forEach(entry -> {
                    double percent = totalDurationMs > 0 ? (entry.durationMs() * 100.0 / totalDurationMs) : 0;
                    String indicator = getPerformanceIndicator(percent);
                    report.append(String.format("  %s %s (%s, %.1f%%)%n",
                            indicator, entry.stageName(), formatDuration(entry.durationMs()), percent));
                    if (entry.details() != null) {
                        report.append(String.format("       └─ %s%n", entry.details()));
                    }
                });

        report.append(System.lineSeparator());
        report.append("Legend: 🔴 >30%  🟡 15-30%  🟢 <15%").append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    private void appendTimeline(StringBuilder report) {
        if (timingEntries.isEmpty()) {
            return;
        }

        report.append("EXECUTION TIMELINE:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());

        long cumulative = 0;
        for (TimingEntry entry : timingEntries) {
            if (entry.depth() == 0) {
                double startPercent = totalDurationMs > 0 ? (cumulative * 100.0 / totalDurationMs) : 0;
                double endPercent = totalDurationMs > 0 ? ((cumulative + entry.durationMs()) * 100.0 / totalDurationMs) : 0;
                String timeline = createTimeline(startPercent, endPercent);
                report.append(String.format("  %-30s %s%n", truncate(entry.stageName(), 30), timeline));
                cumulative += entry.durationMs();
            }
        }

        report.append("  ").append("0%").append(" ".repeat(36)).append("50%").append(" ".repeat(35)).append("100%").append(System.lineSeparator());
        report.append(System.lineSeparator());
    }

    private void appendDetailedLog(StringBuilder report) {
        report.append("=".repeat(80)).append(System.lineSeparator());
        report.append("DETAILED LOG:").append(System.lineSeparator());
        report.append("-".repeat(80)).append(System.lineSeparator());
        report.append(builder);
    }

    private String createBar(double percent) {
        int filled = (int) Math.round(percent / 100.0 * BAR_WIDTH);
        filled = Math.min(filled, BAR_WIDTH);
        return String.valueOf(BAR_CHAR).repeat(filled) + String.valueOf(BAR_EMPTY).repeat(BAR_WIDTH - filled);
    }

    private String createTimeline(double startPercent, double endPercent) {
        int start = (int) Math.round(startPercent / 100.0 * BAR_WIDTH);
        int end = (int) Math.round(endPercent / 100.0 * BAR_WIDTH);
        start = Math.max(0, Math.min(start, BAR_WIDTH));
        end = Math.max(start, Math.min(end, BAR_WIDTH));

        return "·".repeat(start) + "█".repeat(end - start) + "·".repeat(BAR_WIDTH - end);
    }

    private String getPerformanceIndicator(double percent) {
        if (percent > 30) return "🔴";
        if (percent > 15) return "🟡";
        return "🟢";
    }

    private String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else if (ms < 60000) {
            return String.format("%.1f sec", ms / 1000.0);
        } else {
            return String.format("%.1f min", ms / 60000.0);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen - 3) + "...";
    }

    public record TimingEntry(String stageName, long durationMs, String details, int depth, String parentStage) {
        public TimingEntry(String stageName, long durationMs, String details) {
            this(stageName, durationMs, details, 0, null);
        }
    }

    public static class Timer implements AutoCloseable {
        private final PdfGenerationLog log;
        private final String stageName;
        private final long startTime;
        private String details;

        public Timer(@NotNull PdfGenerationLog log, @NotNull String stageName) {
            this.log = log;
            this.stageName = stageName;
            this.startTime = System.currentTimeMillis();
            log.timerStack.push(stageName);
        }

        public Timer withDetails(String details) {
            this.details = details;
            return this;
        }

        @Override
        public void close() {
            log.timerStack.pop();
            long duration = System.currentTimeMillis() - startTime;
            log.recordTiming(stageName, duration, details);
        }
    }

    public Timer startTimer(@NotNull String stageName) {
        return new Timer(this, stageName);
    }

    public <T> T timed(@NotNull String stageName, @NotNull java.util.function.Supplier<T> supplier) {
        try (Timer ignored = startTimer(stageName)) {
            return supplier.get();
        }
    }

    public <T> T timed(@NotNull String stageName, @NotNull java.util.function.Supplier<T> supplier, @NotNull java.util.function.Function<T, String> detailsProvider) {
        try (Timer timer = startTimer(stageName)) {
            T result = supplier.get();
            timer.withDetails(detailsProvider.apply(result));
            return result;
        }
    }

    public void timed(@NotNull String stageName, @NotNull Runnable runnable) {
        try (Timer ignored = startTimer(stageName)) {
            runnable.run();
        }
    }
}
