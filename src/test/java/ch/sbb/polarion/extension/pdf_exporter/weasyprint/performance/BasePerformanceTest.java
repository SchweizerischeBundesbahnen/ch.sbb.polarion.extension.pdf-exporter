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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * The base of the performance tests: they export documents of a known shape and fail when an export takes longer than
 * it does today. They run in the profile {@code performance-tests-with-weasyprint-docker} alone.
 * <p>
 * The exporter and WeasyPrint are timed apart, read from the timings of the generation log, so a failure says which side
 * became slow. Each document is exported {@link #RUNS} times, and the time of each part is the average. Each part has a
 * reference time in {@code performance/reference-times.properties}, what it took on the machine the reference times were
 * taken on. {@link PerformanceRun} measures how much slower this machine does a fixed piece of work, which runs no code
 * of the exporter, and expects each part to take its reference time times that factor here: a change which slows every
 * export cannot slow the measure of the machine with it. The exporter fails at twice its expected time, WeasyPrint at
 * three times, and either is a warning above one and a half times. The run writes a report of every export after the
 * last test.
 * </p>
 * <p>
 * The CSS of an export carries its fonts as Polarion gives them, embedded as data URLs, as a real export does. A cost
 * which grows with the CSS shows here as it shows in Polarion.
 * </p>
 */
@Tag("performance")
@ExtendWith(PerformanceRun.Extension.class)
public abstract class BasePerformanceTest extends BasePdfConverterTest {

    private static final String WEASYPRINT_STAGE = "WeasyPrint conversion";

    /** How many times each document is exported, the time of a part being the average of all. */
    protected static final int RUNS = 3;

    /** The fonts of the default CSS, which Polarion serves and the export embeds, in place of fonts of a like size. */
    private static final String POLARION_FONTS = "/polarion/ria/fonts/";
    private static final String FONT_AWESOME = "/polarion/ria/fontawesome-";

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
     * Exports the content as a LiveDoc {@link #RUNS} times and returns the average time of each part: one export of a
     * document takes too little for one run to tell a slower exporter from a busy machine. The first export of a run of
     * the tests is preceded by one which is not timed, to warm the JVM and the service up. The timing report of each
     * export is written to the reports folder at once, so that it is there whichever check fails.
     */
    protected @NotNull Timing export(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        if (!warmedUp) {
            warmedUp = true;
            exportOnce("warmup", "Warm-up", readHtmlResource("performance/reference"), portraitA4().build());
        }
        long totalMs = 0;
        long weasyPrintMs = 0;
        Timing last = null;
        for (int run = 1; run <= RUNS; run++) {
            last = exportOnce(name + "-" + run, title, content, params);
            totalMs += last.totalMs();
            weasyPrintMs += last.weasyPrintMs();
        }
        return new Timing(name, last.pdf(), Math.round((double) totalMs / RUNS), Math.round((double) weasyPrintMs / RUNS), last.report());
    }

    private @NotNull Timing exportOnce(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
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

    @SneakyThrows
    private static void writeReport(@NotNull String name, @NotNull String text) {
        Files.writeString(Path.of(REPORTS_FOLDER_PATH, "performance-" + name + ".txt"), text, StandardCharsets.UTF_8);
    }

    /**
     * Fails when the exporter took longer than {@link PerformanceRun#EXPORTER_TOLERANCE} times its expected time, or
     * WeasyPrint {@link PerformanceRun#WEASYPRINT_TOLERANCE} times its own. The expected time is the reference time of the
     * export times the factor of this machine. A part above {@link PerformanceRun#WARNING_TOLERANCE} times its expected time
     * is only marked in the table and in the log. Both parts go into the table of the run first, and the timing report of
     * the export is written to the reports folder either way.
     */
    protected void assertWithinReference(@NotNull Timing timing) {
        PerformanceRun run = PerformanceRun.current();
        run.add(timing.name(), PerformanceRun.EXPORTER, timing.exporterMs(), PerformanceRun.EXPORTER_TOLERANCE);
        run.add(timing.name(), PerformanceRun.WEASYPRINT, timing.weasyPrintMs(), PerformanceRun.WEASYPRINT_TOLERANCE);
        long exporterLimit = run.limit(timing.name(), PerformanceRun.EXPORTER, PerformanceRun.EXPORTER_TOLERANCE);
        long weasyPrintLimit = run.limit(timing.name(), PerformanceRun.WEASYPRINT, PerformanceRun.WEASYPRINT_TOLERANCE);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, average of %d exports, expected times %.2f times the reference".formatted(
                timing.name(), timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, RUNS, run.factor());
        writeReport(timing.name(), summary + System.lineSeparator() + timing.report());

        assertThat(timing.exporterMs()).as("The exporter is within its limit. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(exporterLimit);
        assertThat(timing.weasyPrintMs()).as("WeasyPrint is within its limit. %s%n%s", summary, timing.report()).isLessThanOrEqualTo(weasyPrintLimit);
    }

    @SneakyThrows
    protected static int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
