package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGenerationLogTest {

    private PdfGenerationLog log;

    @BeforeEach
    void setUp() {
        log = new PdfGenerationLog();
    }

    @Test
    void shouldLogMessages() {
        log.log("Test message 1");
        log.log("Test message 2");

        String logContent = log.getLog();
        assertThat(logContent).contains("Test message 1");
        assertThat(logContent).contains("Test message 2");
    }

    @Test
    void shouldRecordTiming() {
        log.recordTiming("Stage 1", 100);
        log.recordTiming("Stage 2", 200, "with details");

        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).stageName()).isEqualTo("Stage 1");
        assertThat(entries.get(0).durationMs()).isEqualTo(100);
        assertThat(entries.get(0).details()).isNull();
        assertThat(entries.get(1).stageName()).isEqualTo("Stage 2");
        assertThat(entries.get(1).durationMs()).isEqualTo(200);
        assertThat(entries.get(1).details()).isEqualTo("with details");
    }

    @Test
    void shouldTrackHierarchicalTimings() {
        try (PdfGenerationLog.Timer outer = log.startTimer("Outer")) {
            try (PdfGenerationLog.Timer inner = log.startTimer("Inner")) {
                // Simulate work
            }
        }

        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(2);

        PdfGenerationLog.TimingEntry innerEntry = entries.get(0);
        assertThat(innerEntry.stageName()).isEqualTo("Inner");
        assertThat(innerEntry.depth()).isEqualTo(1);
        assertThat(innerEntry.parentStage()).isEqualTo("Outer");

        PdfGenerationLog.TimingEntry outerEntry = entries.get(1);
        assertThat(outerEntry.stageName()).isEqualTo("Outer");
        assertThat(outerEntry.depth()).isEqualTo(0);
        assertThat(outerEntry.parentStage()).isNull();
    }

    @Test
    void shouldSetHtmlSize() {
        log.setHtmlSize(1024);
        log.finish();

        String report = log.generateTimingReport("Test Doc");
        assertThat(report).contains("HTML Size:");
        assertThat(report).contains("1.0 KB");
    }

    @Test
    void shouldSetPdfMetrics() {
        log.setPdfMetrics(2048, 5, "PDF/A-2b");
        log.finish();

        String report = log.generateTimingReport("Test Doc");
        assertThat(report).contains("PDF Size:");
        assertThat(report).contains("2.0 KB");
        assertThat(report).contains("Page Count:");
        assertThat(report).contains("5");
        assertThat(report).contains("PDF Variant:");
        assertThat(report).contains("PDF/A-2b");
    }

    @Test
    void shouldCalculateTotalDuration() throws InterruptedException {
        Thread.sleep(10);
        log.finish();

        assertThat(log.getTotalDurationMs()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldGenerateTimingReport() {
        log.recordTiming("HTML Preparation", 500);
        log.recordTiming("WeasyPrint conversion", 2000);
        log.recordTiming("PDF/A-1 post-processing", 300);
        log.setHtmlSize(50000);
        log.setPdfMetrics(100000, 10, "PDF/A-1b");
        log.finish();

        String report = log.generateTimingReport("Test Document");

        assertThat(report).contains("PDF GENERATION TIMING REPORT");
        assertThat(report).contains("Document: Test Document");
        assertThat(report).contains("SUMMARY STATISTICS:");
        assertThat(report).contains("TIMING BREAKDOWN:");
        assertThat(report).contains("TIME BY CATEGORY:");
        assertThat(report).contains("SLOWEST STAGES");
        assertThat(report).contains("EXECUTION TIMELINE:");
        assertThat(report).contains("DETAILED LOG:");
    }

    @Test
    void shouldCategorizeTimingEntries() {
        log.recordTiming("HTML content preparation", 100);
        log.recordTiming("WeasyPrint conversion", 500);
        log.recordTiming("PDF/A-1 post-processing", 200);
        log.recordTiming("Cover page generation", 150);
        log.finish();

        String report = log.generateTimingReport("Test");

        assertThat(report).contains("HTML Processing");
        assertThat(report).contains("WeasyPrint Conversion");
        assertThat(report).contains("PDF Post-processing");
        assertThat(report).contains("Cover Page");
    }

    @Test
    void shouldShowPerformanceIndicators() {
        log.recordTiming("Fast stage", 100);
        log.recordTiming("Slow stage", 5000);
        log.finish();

        String report = log.generateTimingReport("Test");

        assertThat(report).contains("Legend:");
    }

    @Test
    void shouldHandleEmptyTimingEntries() {
        log.finish();

        String report = log.generateTimingReport("Empty Test");

        assertThat(report).contains("PDF GENERATION TIMING REPORT");
        assertThat(report).contains("Document: Empty Test");
    }

    @Test
    void shouldHandleNullDocumentTitle() {
        log.finish();

        String report = log.generateTimingReport(null);

        assertThat(report).contains("Document: Unknown");
    }

    @Test
    void shouldReturnCopyOfTimingEntries() {
        log.recordTiming("Stage 1", 100);

        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        entries.clear();

        assertThat(log.getTimingEntries()).hasSize(1);
    }

    @Test
    void timerShouldRecordDurationOnClose() throws InterruptedException {
        try (PdfGenerationLog.Timer timer = log.startTimer("Test Stage")) {
            Thread.sleep(10);
        }

        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).stageName()).isEqualTo("Test Stage");
        assertThat(entries.get(0).durationMs()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void timerShouldSupportDetails() {
        try (PdfGenerationLog.Timer timer = log.startTimer("Test Stage").withDetails("extra info")) {
            // Simulate work
        }

        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).details()).isEqualTo("extra info");
    }

    @Test
    void shouldCalculateAveragePerPage() throws InterruptedException {
        log.setPdfMetrics(1024, 4, "PDF/A-2b");
        Thread.sleep(10);
        log.finish();

        String report = log.generateTimingReport("Test");
        assertThat(report).contains("Avg per Page:");
    }

    @Test
    void shouldCalculateThroughput() throws InterruptedException {
        log.setHtmlSize(102400); // 100 KB
        log.recordTiming("Stage", 1000);
        Thread.sleep(10);
        log.finish();

        String report = log.generateTimingReport("Test");
        assertThat(report).contains("Throughput:");
        assertThat(report).contains("KB/s");
    }

    @Test
    void timingEntryShouldSupportSimpleConstructor() {
        PdfGenerationLog.TimingEntry entry = new PdfGenerationLog.TimingEntry("Stage", 100, "details");

        assertThat(entry.stageName()).isEqualTo("Stage");
        assertThat(entry.durationMs()).isEqualTo(100);
        assertThat(entry.details()).isEqualTo("details");
        assertThat(entry.depth()).isZero();
        assertThat(entry.parentStage()).isNull();
    }

    @Test
    void timedWithSupplierShouldRecordTimingAndReturnResult() {
        String result = log.timed("Test operation", () -> "test result");

        assertThat(result).isEqualTo("test result");
        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).stageName()).isEqualTo("Test operation");
    }

    @Test
    void timedWithRunnableShouldRecordTiming() {
        StringBuilder sb = new StringBuilder();
        log.timed("Test operation", () -> sb.append("executed"));

        assertThat(sb.toString()).isEqualTo("executed");
        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).stageName()).isEqualTo("Test operation");
    }

    @Test
    void timedWithDetailsProviderShouldRecordTimingWithDetails() {
        String result = log.timed("Test operation",
                () -> "result value",
                value -> "length=" + value.length());

        assertThat(result).isEqualTo("result value");
        List<PdfGenerationLog.TimingEntry> entries = log.getTimingEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).stageName()).isEqualTo("Test operation");
        assertThat(entries.get(0).details()).isEqualTo("length=12");
    }
}
