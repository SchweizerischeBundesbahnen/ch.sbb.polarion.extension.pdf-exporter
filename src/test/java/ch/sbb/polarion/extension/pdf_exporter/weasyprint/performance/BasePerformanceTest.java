package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.generic.util.ExecutionProfiler;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * The base of the performance tests: they export documents of a known shape and fail when an export takes far longer
 * than it does today. They run in the profile {@code performance-tests} alone.
 * <p>
 * A budget is set for the exporter and for WeasyPrint apart, read from the timings of the generation log, so a failure
 * says which side became slow. The budgets are some ten times what an export takes on the machine they were set on.
 * They are scaled by how much slower the machine of the run exports a small reference document, so a slower machine
 * is not taken for a slower export, while an export grown an order of magnitude slower still fails.
 * </p>
 */
@Tag("performance")
public abstract class BasePerformanceTest extends BasePdfConverterTest {

    /** What the reference document takes to export on the machine the budgets were set on, an arm64 Mac, in ms. */
    private static final long REFERENCE_MS = 850;

    private static final String WEASYPRINT_STAGE = "WeasyPrint conversion";

    /** How much slower than the machine of the budgets this one exports, measured once per run. */
    private static Double slowdown;

    /** An export and how long its parts took. */
    protected record Timing(byte @NotNull [] pdf, long totalMs, long weasyPrintMs, @NotNull String report) {
        long exporterMs() {
            return totalMs - weasyPrintMs;
        }
    }

    protected static @NotNull ExportParams.ExportParamsBuilder<?, ?> portraitA4() {
        return ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4);
    }

    /** Exports the content as a LiveDoc and times its stages. */
    protected @NotNull Timing export(@NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title(title)
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        PdfGenerationLog log = new PdfGenerationLog();
        byte[] pdf = converter.convertToPdf(params, null, log);
        long weasyPrintMs = log.getTimingEntries().stream()
                .filter(entry -> entry.stageName().startsWith(WEASYPRINT_STAGE))
                .mapToLong(ExecutionProfiler.TimingEntry::durationMs)
                .sum();
        return new Timing(pdf, log.getTotalDurationMs(), weasyPrintMs, log.generateTimingReport(title));
    }

    /**
     * Fails when the exporter or WeasyPrint took longer than its budget, scaled to this machine. The timing report of the
     * export is written to the reports folder either way.
     */
    @SneakyThrows
    protected void assertWithinBudget(@NotNull String name, @NotNull Timing timing, long exporterBudgetMs, long weasyPrintBudgetMs) {
        double scale = slowdown();
        long exporterLimit = Math.round(exporterBudgetMs * scale);
        long weasyPrintLimit = Math.round(weasyPrintBudgetMs * scale);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, budgets scaled by %.2f".formatted(
                name, timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, scale);
        Files.writeString(Path.of(REPORTS_FOLDER_PATH, "performance-" + name + ".txt"), summary + System.lineSeparator() + timing.report(), StandardCharsets.UTF_8);

        assertThat(timing.exporterMs()).as("The exporter is within its budget. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(exporterLimit);
        assertThat(timing.weasyPrintMs()).as("WeasyPrint is within its budget. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(weasyPrintLimit);
    }

    /** How much slower than the machine of the budgets this one exports the reference document, never less than one. */
    private double slowdown() {
        if (slowdown == null) {
            String reference = readHtmlResource("performance/reference");
            ExportParams params = portraitA4().build();
            // The first export warms the JVM and the service up, the best of the next three is the time of the machine
            export("Reference", reference, params);
            long best = Long.MAX_VALUE;
            for (int run = 0; run < 3; run++) {
                best = Math.min(best, export("Reference", reference, params).totalMs());
            }
            slowdown = Math.max(1d, (double) best / REFERENCE_MS);
        }
        return slowdown;
    }

    @SneakyThrows
    protected static int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
