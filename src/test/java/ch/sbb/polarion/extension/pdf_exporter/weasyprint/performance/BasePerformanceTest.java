package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.generic.util.ExecutionProfiler;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

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
 * became slow. Each document is exported {@link #RUNS} times, and the time of each part is the median. Each part has a
 * reference time in {@code performance/reference-times-<architecture>.properties}, its average over runs on a machine of
 * that architecture. {@link PerformanceRun}
 * expects each part to take its reference time scaled by how the basket went in this run, a few exports of different
 * shapes timed first, which absorbs how fast this machine and this moment are; the small document it scales by a fixed
 * piece of JDK work, so that a change which slows every export still shows. It writes a report of every export after the
 * last test.
 * </p>
 * <p>
 * The CSS of an export carries its fonts as Polarion gives them, embedded as data URLs, as a real export does. A cost
 * which grows with the CSS shows here as it shows in Polarion.
 * </p>
 */
@Tag("performance")
@ExtendWith(PerformanceRun.Extension.class)
// The tests of the basket take its timings and export nothing, leaving the stubs of their set-up unused
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BasePerformanceTest extends BasePdfConverterTest {

    private static final String WEASYPRINT_STAGE = "WeasyPrint conversion";

    /** How many times each document is exported, the time of a part being the median of all. */
    protected static final int RUNS = 3;

    /** The small document, which takes the exporter little but what every export costs. */
    protected static final BasketExport SMALL_DOCUMENT = new BasketExport(PerformanceRun.SMALL_DOCUMENT, "Small document",
            BasePerformanceTest::smallDocument, () -> portraitA4().build());

    /** A table of many rows, where WeasyPrint spends its time on the layout of a table. */
    protected static final BasketExport LARGE_TABLE = new BasketExport("largeTable", "A large table",
            () -> Documents.largeTable(400), () -> portraitA4().fitToPage(true).build());

    /** Sections which page breaks turn, where WeasyPrint spends its time on pages. */
    protected static final BasketExport PAGE_BREAK_SECTIONS = new BasketExport("pageBreakSections", "Page breaks",
            () -> Documents.pageBreakSections(40), () -> portraitA4().fitToPage(true).build());

    /** Cells running across pages, where WeasyPrint spends its time on breaking text. */
    protected static final BasketExport LONG_CELLS = new BasketExport("longCells", "Long cells",
            () -> Documents.longCells(3, 10_000), () -> portraitA4().fitToPage(true).build());

    /**
     * The exports which {@link PerformanceRun} scales every other export by. Each spends its time on something else, so a
     * runner which is slower at one thing than at another moves the basket by less than it moves one of them.
     */
    private static final List<BasketExport> BASKET = List.of(SMALL_DOCUMENT, LARGE_TABLE, PAGE_BREAK_SECTIONS, LONG_CELLS);

    /** The fonts of the default CSS, which Polarion serves and the export embeds, in place of fonts of a like size. */
    private static final String POLARION_FONTS = "/polarion/ria/fonts/";
    private static final String FONT_AWESOME = "/polarion/ria/fontawesome-";

    /** Font Awesome 6.2.0 as Polarion 2606 serves it: the stylesheet the template links, and its fonts. */
    private static final String FONT_AWESOME_FILES = "/performance/fontawesome-";

    /** The exports of the basket as the run timed them first, after warming the JVM and the service up, by name. */
    private static final Map<String, Timing> basket = new HashMap<>();

    private static String embeddedFont;

    /** An export and how long its parts took. */
    protected record Timing(@NotNull String name, byte @NotNull [] pdf, long totalMs, long weasyPrintMs, @NotNull String report) {
        long exporterMs() {
            return totalMs - weasyPrintMs;
        }
    }

    /** An export of the basket: its name in the reference times, its title, its content and the parameters of its export. */
    protected record BasketExport(@NotNull String name, @NotNull String title, @NotNull Supplier<String> content, @NotNull Supplier<ExportParams> params) {
    }

    /**
     * Links stylesheets as an export in Polarion does: the template links the stylesheet of Font Awesome, which is fetched
     * and embedded with its fonts. The base of the tests leaves links as they are.
     */
    @Override
    protected void setupHelperComponents() {
        super.setupHelperComponents();
        htmlProcessor = new HtmlProcessor(fileResourceProvider, localizationSettings, new HtmlLinksHelper(fileResourceProvider));
    }

    /**
     * Serves what Polarion serves an export: the stylesheet and the fonts of Font Awesome as they are, the fonts of the
     * default CSS as a font of a like size, which the test CSS overrides for every element. Nothing is left as a URL in the CSS.
     */
    @BeforeEach
    void serveTheResourcesOfPolarion() {
        lenient().when(fileResourceProvider.getResourceAsBytes(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return url.startsWith(FONT_AWESOME) ? fontAwesomeFile(url) : null;
        });
        lenient().when(fileResourceProvider.getResourceAsBase64String(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.startsWith(FONT_AWESOME)) {
                byte[] font = fontAwesomeFile(url);
                return font == null ? null : "data:font/%s;base64,%s".formatted(url.endsWith(".woff2") ? "woff2" : "ttf", Base64.getEncoder().encodeToString(font));
            }
            return url.startsWith(POLARION_FONTS) ? embeddedFont() : null;
        });
    }

    @SneakyThrows
    private static byte @Nullable [] fontAwesomeFile(@NotNull String url) {
        try (InputStream file = BasePerformanceTest.class.getResourceAsStream(FONT_AWESOME_FILES + url.substring(FONT_AWESOME.length()))) {
            return file == null ? null : file.readAllBytes();
        }
    }

    @SneakyThrows
    private static @NotNull String embeddedFont() {
        if (embeddedFont == null) {
            // A font of the size of those of Polarion, from 200 to 430 KB each, which the test CSS overrides for every element
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
     * Exports the content as a LiveDoc {@link #RUNS} times and returns the median time of each part: one export of a
     * document takes too little for one run to tell a slower exporter from a busy machine, and one export which a pause
     * of the runner slows does not move the median, as it moves an average. The timing report of each export is written
     * to the reports folder at once, so that it is there whichever check fails.
     * <p>
     * Before the first export of a run of the tests, exports which are not timed warm the JVM and the service up, see
     * {@link #timeTheBasket()}, and the exports of the basket are timed, which {@link PerformanceRun} scales the other reference times by. An
     * export of the basket is not taken here: {@link #export(BasketExport)} takes it.
     * </p>
     */
    protected @NotNull Timing export(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        timeTheBasket();
        if (basket.containsKey(name)) {
            throw new IllegalArgumentException(name + " is an export of the basket, which the run timed before its other exports");
        }
        return median(name, title, content, params);
    }

    /**
     * The timing of an export of the basket, which the run took before its first other export rather than one of its own:
     * the reference times hold each export against the basket of its run, so the run must scale by the very sample it
     * reports.
     */
    protected @NotNull Timing export(@NotNull BasketExport export) {
        timeTheBasket();
        return basket.get(export.name());
    }

    /**
     * Warms the JVM and the service up and times the basket, once for the run, before the first other export is timed.
     * The first exports of a large document after the small one take the exporter up to half as long again, over all of
     * their {@link #RUNS}, so each document of the basket is exported once untimed first.
     */
    protected void timeTheBasket() {
        if (basket.isEmpty()) {
            for (int run = 1; run <= RUNS; run++) {
                exportOnce("warmup-" + run, "Warm-up", smallDocument(), portraitA4().build());
            }
            for (BasketExport export : BASKET) {
                if (export != SMALL_DOCUMENT) {
                    exportOnce("warmup-" + export.name(), export.title(), export.content().get(), export.params().get());
                }
            }
            Map<String, Timing> timed = new HashMap<>();
            for (BasketExport export : BASKET) {
                Timing timing = median(export.name(), export.title(), export.content().get(), export.params().get());
                timed.put(export.name(), timing);
                PerformanceRun.current().baseline(export.name(), timing.exporterMs(), timing.weasyPrintMs());
            }
            basket.putAll(timed);
        }
    }

    /** The small document, which takes the exporter and WeasyPrint little but what every export costs. */
    protected static @NotNull String smallDocument() {
        return readHtmlResource("performance/reference");
    }

    /** The median of each part: of the exporter and of WeasyPrint apart, which may come from different exports. */
    private @NotNull Timing median(@NotNull String name, @NotNull String title, @NotNull String content, @NotNull ExportParams params) {
        long[] exporterMs = new long[RUNS];
        long[] weasyPrintMs = new long[RUNS];
        Timing last = null;
        for (int run = 1; run <= RUNS; run++) {
            last = exportOnce(name + "-" + run, title, content, params);
            exporterMs[run - 1] = last.exporterMs();
            weasyPrintMs[run - 1] = last.weasyPrintMs();
        }
        long weasyPrintMedian = median(weasyPrintMs);
        return new Timing(name, last.pdf(), median(exporterMs) + weasyPrintMedian, weasyPrintMedian, last.report());
    }

    /** The middle of the times, or the mean of the two middle ones where their number is even. */
    protected static long median(long @NotNull [] timesMs) {
        long[] sorted = timesMs.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : Math.round((sorted[middle - 1] + sorted[middle]) / 2.0);
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
    protected static void writeReport(@NotNull String name, @NotNull String text) {
        Files.writeString(Path.of(REPORTS_FOLDER_PATH, "performance-" + name + ".txt"), text, StandardCharsets.UTF_8);
    }

    /**
     * Fails when a part of the export took longer than its limit, a multiple of the time {@link PerformanceRun} expects
     * of it on this machine, and only marks it in the report above its warning level. Both parts go into the report of
     * the run first, and the timing report of the export is written to the reports folder either way.
     */
    protected void assertWithinReference(@NotNull Timing timing) {
        PerformanceRun run = PerformanceRun.current();
        run.add(timing.name(), PerformanceRun.EXPORTER, timing.exporterMs());
        run.add(timing.name(), PerformanceRun.WEASYPRINT, timing.weasyPrintMs());
        long exporterLimit = run.limit(timing.name(), PerformanceRun.EXPORTER);
        long weasyPrintLimit = run.limit(timing.name(), PerformanceRun.WEASYPRINT);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, median of %d exports".formatted(
                timing.name(), timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, RUNS);
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
