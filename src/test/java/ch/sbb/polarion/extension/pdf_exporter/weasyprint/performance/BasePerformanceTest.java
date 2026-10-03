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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * The base of the performance tests: they export documents of a known shape and fail when an export takes longer than
 * it does today. They run in the profile {@code performance-tests-with-weasyprint-docker} alone.
 * <p>
 * A budget is set for the exporter and for WeasyPrint apart, read from the timings of the generation log, so a failure
 * says which side became slow. The budgets are twice what an export takes on the machine they were set on. They are
 * scaled by how much slower the machine of the run does a fixed piece of work, which runs no code of the exporter: a
 * change which slows every export cannot slow the measure of the machine with it and raise every budget.
 * </p>
 * <p>
 * The CSS of an export carries its fonts as Polarion gives them, embedded as data URLs, as a real export does. A cost
 * which grows with the CSS shows here as it shows in Polarion.
 * </p>
 */
@Tag("performance")
public abstract class BasePerformanceTest extends BasePdfConverterTest {

    /** What the fixed piece of work takes on the machine the budgets were set on, an arm64 Mac, in ms. */
    private static final long CALIBRATION_MS = 265;

    private static final String WEASYPRINT_STAGE = "WeasyPrint conversion";

    private static final String MACHINE = "machine";

    /** The fonts of the default CSS, which Polarion serves and the export embeds, in place of fonts of a like size. */
    private static final String POLARION_FONTS = "/polarion/ria/fonts/";
    private static final String FONT_AWESOME = "/polarion/ria/fontawesome-";

    /** How much slower than the machine of the budgets this one is, measured once per run. */
    private static Double slowdown;

    /** Whether the exporter and the service ran once in this JVM, so that no timed export pays for a cold start. */
    private static boolean warmedUp;

    private static String embeddedFont;

    /** An export and how long its parts took. */
    protected record Timing(@NotNull String name, byte @NotNull [] pdf, long totalMs, long weasyPrintMs, @NotNull String report) {
        long exporterMs() {
            return totalMs - weasyPrintMs;
        }
    }

    /** Embeds the fonts the default CSS names, as Polarion serves them, rather than leaving their URLs in the CSS. */
    @BeforeEach
    void embedTheFontsOfTheDefaultCss() {
        lenient().when(fileResourceProvider.getResourceAsBase64String(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return url.startsWith(POLARION_FONTS) || url.startsWith(FONT_AWESOME) ? embeddedFont() : null;
        });
    }

    @SneakyThrows
    private static @NotNull String embeddedFont() {
        if (embeddedFont == null) {
            // A font of the size of those of Polarion, from 300 to 430 KB each, which the test CSS overrides for every element
            try (InputStream font = BasePerformanceTest.class.getResourceAsStream(WEASYPRINT_TEST_FONT_RESOURCES_FOLDER + "fa-solid-900" + EXT_TTF)) {
                embeddedFont = "data:font/ttf;base64," + Base64.getEncoder().encodeToString(Objects.requireNonNull(font).readAllBytes());
            }
        }
        return embeddedFont;
    }

    protected static @NotNull ExportParams.ExportParamsBuilder<?, ?> portraitA4() {
        return ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4);
    }

    /**
     * Exports the content as a LiveDoc and times its stages. The first export of a run is preceded by one which is not
     * timed, to warm the JVM and the service up. The timing report is written to the reports folder at once, so that it is
     * there whichever check fails.
     */
    protected @NotNull Timing export(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        if (!warmedUp) {
            warmedUp = true;
            export("warmup", "Warm-up", readHtmlResource("performance/reference"), portraitA4().build());
        }
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
        Timing timing = new Timing(name, pdf, log.getTotalDurationMs(), weasyPrintMs, log.generateTimingReport(title));
        writeReport(timing.name(), "%s: exporter %d ms, WeasyPrint %d ms%n%s".formatted(name, timing.exporterMs(), timing.weasyPrintMs(), timing.report()));
        return timing;
    }

    /**
     * Exports the content once to warm the JVM and the service up for it, then three times, and returns the fastest: the
     * export of a small document takes too little for one run to tell a slower exporter from a busy machine.
     */
    protected @NotNull Timing fastestOfThree(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        export(name + "-warmup", title, content, params);
        Timing fastest = null;
        for (int run = 1; run <= 3; run++) {
            Timing timing = export(name + "-" + run, title, content, params);
            if (fastest == null || timing.totalMs() < fastest.totalMs()) {
                fastest = timing;
            }
        }
        return new Timing(name, fastest.pdf(), fastest.totalMs(), fastest.weasyPrintMs(), fastest.report());
    }

    @SneakyThrows
    private static void writeReport(@NotNull String name, @NotNull String text) {
        Files.writeString(Path.of(REPORTS_FOLDER_PATH, "performance-" + name + ".txt"), text, StandardCharsets.UTF_8);
    }

    /**
     * Fails when the exporter or WeasyPrint took longer than its budget, scaled to this machine. The timing report of the
     * export is written to the reports folder either way.
     */
    protected void assertWithinBudget(@NotNull Timing timing, long exporterBudgetMs, long weasyPrintBudgetMs) {
        double scale = slowdown();
        long exporterLimit = Math.round(exporterBudgetMs * scale);
        long weasyPrintLimit = Math.round(weasyPrintBudgetMs * scale);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, budgets scaled by %.2f".formatted(
                timing.name(), timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, scale);
        writeReport(timing.name(), summary + System.lineSeparator() + timing.report());

        assertThat(timing.exporterMs()).as("The exporter is within its budget. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(exporterLimit);
        assertThat(timing.weasyPrintMs()).as("WeasyPrint is within its budget. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(weasyPrintLimit);
    }

    /** How much slower than the machine of the budgets this one does the fixed piece of work, never less than one. */
    private static double slowdown() {
        if (slowdown == null) {
            calibrate();
            long best = Long.MAX_VALUE;
            for (int run = 0; run < 3; run++) {
                best = Math.min(best, calibrate());
            }
            slowdown = Math.max(1d, (double) best / CALIBRATION_MS);
            writeReport(MACHINE, "machine: best of three %d ms of fixed work, against %d ms on the machine of the budgets, budgets scaled by %.2f%n"
                    .formatted(best, CALIBRATION_MS, slowdown));
        }
        return slowdown;
    }

    /**
     * Times a fixed piece of work of the JDK alone, hashing and sorting, which no change of the exporter or of its
     * libraries makes slower. It tells how fast this machine is, for the budgets.
     */
    @SneakyThrows
    private static long calibrate() {
        long start = System.nanoTime();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] block = new byte[1 << 20];
        new Random(42).nextBytes(block);
        for (int round = 0; round < 200; round++) {
            digest.update(block);
        }
        int[] numbers = new Random(42).ints(4_000_000).toArray();
        Arrays.sort(numbers);
        consume(digest.digest(), numbers);
        return (System.nanoTime() - start) / 1_000_000;
    }

    /** Keeps the result of the work, so that the JIT cannot drop the work. */
    private static void consume(byte @NotNull [] hash, int @Nullable [] numbers) {
        assertThat(hash).hasSize(32);
        assertThat(numbers).isNotEmpty();
    }

    @SneakyThrows
    protected static int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
