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
 * became slow. Each has a reference time, what it took on the machine the reference times were taken on, and may take
 * three times as long. {@link PerformanceRun} scales the reference times by how much slower the machine of the run does
 * a fixed piece of work, which runs no code of the exporter: a change which slows every export cannot slow the measure of
 * the machine with it and raise every limit. It writes a table of every export to the log after the last test.
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
     * Fails when the exporter or WeasyPrint took longer than {@link PerformanceRun#TOLERANCE} times its reference time,
     * scaled to this machine. Both parts go into the table of the run first, and the timing report of the export is
     * written to the reports folder either way.
     *
     * @param exporterReferenceMs   what the exporter took on the machine of the reference times
     * @param weasyPrintReferenceMs what WeasyPrint took there
     */
    protected void assertWithinReference(@NotNull Timing timing, long exporterReferenceMs, long weasyPrintReferenceMs) {
        PerformanceRun run = PerformanceRun.current();
        run.add(timing.name(), "exporter", timing.exporterMs(), exporterReferenceMs);
        run.add(timing.name(), "WeasyPrint", timing.weasyPrintMs(), weasyPrintReferenceMs);
        long exporterLimit = run.limit(exporterReferenceMs);
        long weasyPrintLimit = run.limit(weasyPrintReferenceMs);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, reference times scaled by %.2f".formatted(
                timing.name(), timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, run.scale());
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
