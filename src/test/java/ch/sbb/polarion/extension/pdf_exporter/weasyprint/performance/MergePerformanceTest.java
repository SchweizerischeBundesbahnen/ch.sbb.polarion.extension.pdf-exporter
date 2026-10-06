package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.pdf_exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.ApiKeyProvider;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import com.polarion.alm.tracker.model.IModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Merges of many documents into one PDF through the bulk processing service, which a collection or a Bulk PDF Export
 * widget makes, timed as the single exports are (#1113), against the service of {@link TimedBulkProcessingService}.
 * <p>
 * A merge is timed in three parts. The exporter prepares the HTML of each document in Polarion. WeasyPrint renders each
 * document, timed by the proxy it is called through, against the same WeasyPrint the single exports go to. The bulk
 * processing service is the rest of the call of the connector: the upload of each document, what the service does with
 * it, and the merge of the PDFs.
 * </p>
 */
class MergePerformanceTest extends BasePerformanceTest {

    private TimedConnector connector;

    /** A merge and how long its parts took. */
    private record MergeTiming(@NotNull String name, byte @NotNull [] pdf, long exporterMs, long weasyPrintMs, long bpsMs) {
    }

    /** The converter of the base, merging through the service in the container and timing the service. */
    @Override
    protected void setupConverter() {
        connector = new TimedConnector(new BulkProcessingServiceConnector(TimedBulkProcessingService.url(),
                getWeasyPrintServiceUrl(), new ApiKeyProvider(() -> null, "bulk processing service")));
        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(placeholderProcessor, velocityEvaluator, getWeasyPrintServiceConnector(),
                coverPageSettings, new PdfTemplateProcessor(), htmlProcessor);
        converter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, placeholderProcessor, velocityEvaluator,
                coverPageProcessor, getWeasyPrintServiceConnector(), htmlProcessor, new PdfTemplateProcessor(), connector);
    }

    /** Many documents of one page, where what each document costs, its upload and rendering, outweighs its content. */
    @Test
    void mergesManySmallDocuments() {
        MergeTiming timing = merge("mergeSmallDocuments", 10, index -> smallDocument());

        assertThat(pageCount(timing.pdf())).isEqualTo(10);
        assertWithinReference(timing);
    }

    /** Fewer documents of a large table each, where rendering them outweighs what each document costs. */
    @Test
    void mergesDocumentsOfALargeTable() {
        MergeTiming timing = merge("mergeLargeTables", 5, index -> Documents.largeTable(60));

        assertThat(pageCount(timing.pdf())).isGreaterThanOrEqualTo(10);
        assertWithinReference(timing);
    }

    /**
     * Merges the documents {@link #RUNS} times and returns the average time of each part, after a merge of two of them
     * which is not timed: the first merge of a run warms the service up as the first export warms WeasyPrint.
     * <p>
     * The time grows with the number of documents, each uploaded and rendered in turn, so a few documents show a slower
     * upload, rendering or merge as well as more would, in a part of the time.
     * </p>
     */
    private @NotNull MergeTiming merge(@NotNull String name, int documents, @NotNull IntFunction<String> content) {
        timeTheBaseline();
        Long networkMs = TimedBulkProcessingService.networkMs();
        if (networkMs != null) {
            PerformanceRun.current().network(networkMs);
        }
        List<ExportParams> params = new ArrayList<>();
        for (int index = 0; index < documents; index++) {
            params.add(liveDoc("Document " + (index + 1), content.apply(index)));
        }

        mergeOnce(name + "-warmup", params.subList(0, 2));
        long exporterMs = 0;
        long weasyPrintMs = 0;
        long bpsMs = 0;
        MergeTiming last = null;
        for (int run = 1; run <= RUNS; run++) {
            last = mergeOnce(name + "-" + run, params);
            exporterMs += last.exporterMs();
            weasyPrintMs += last.weasyPrintMs();
            bpsMs += last.bpsMs();
        }
        return new MergeTiming(name, last.pdf(), average(exporterMs), average(weasyPrintMs), average(bpsMs));
    }

    private static long average(long totalMs) {
        return Math.round((double) totalMs / RUNS);
    }

    private @NotNull MergeTiming mergeOnce(@NotNull String name, @NotNull List<ExportParams> params) {
        long weasyPrintBefore = TimedBulkProcessingService.weasyPrintMs();
        long start = System.nanoTime();
        BulkProcessingConnector.MergeResult result = converter.convertMergedToPdf(params);
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        long weasyPrintMs = TimedBulkProcessingService.weasyPrintMs() - weasyPrintBefore;
        assertThat(result.failedDocumentCount()).as("Every document of %s is merged", name).isZero();
        MergeTiming timing = new MergeTiming(name, result.pdfBytes(), totalMs - connector.lastMs(), weasyPrintMs, connector.lastMs() - weasyPrintMs);
        writeReport(name, "%s: exporter %d ms, WeasyPrint %d ms, BPS %d ms%n".formatted(name, timing.exporterMs(), timing.weasyPrintMs(), timing.bpsMs()));
        return timing;
    }

    /** The export parameters of a document of the merge, its content served as that of a LiveDoc. */
    private @NotNull ExportParams liveDoc(@NotNull String title, @NotNull String content) {
        ExportParams params = portraitA4().locationPath("_default/" + title).documentType(DocumentType.LIVE_DOC).fitToPage(true).build();
        DocumentData<IModule> document = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", title))
                .title(title)
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(document);
        return params;
    }

    /** Fails when a part of the merge took longer than its limit, as {@link #assertWithinReference(Timing)} does for an export. */
    private void assertWithinReference(@NotNull MergeTiming timing) {
        PerformanceRun run = PerformanceRun.current();
        run.add(timing.name(), PerformanceRun.EXPORTER, timing.exporterMs());
        run.add(timing.name(), PerformanceRun.WEASYPRINT, timing.weasyPrintMs());
        run.add(timing.name(), PerformanceRun.BPS, timing.bpsMs());
        long exporterLimit = run.limit(timing.name(), PerformanceRun.EXPORTER);
        long weasyPrintLimit = run.limit(timing.name(), PerformanceRun.WEASYPRINT);
        long bpsLimit = run.limit(timing.name(), PerformanceRun.BPS);
        String summary = "%s: exporter %d ms of %d, WeasyPrint %d ms of %d, BPS %d ms of %d, average of %d merges".formatted(
                timing.name(), timing.exporterMs(), exporterLimit, timing.weasyPrintMs(), weasyPrintLimit, timing.bpsMs(), bpsLimit, RUNS);
        writeReport(timing.name(), summary);

        assertThat(timing.exporterMs()).as("The exporter is within its limit. %s", summary).isLessThanOrEqualTo(exporterLimit);
        assertThat(timing.weasyPrintMs()).as("WeasyPrint is within its limit. %s", summary).isLessThanOrEqualTo(weasyPrintLimit);
        assertThat(timing.bpsMs()).as("The bulk processing service is within its limit. %s", summary).isLessThanOrEqualTo(bpsLimit);
    }

    /** The connector of the service, which keeps how long its last merge took. */
    private static final class TimedConnector implements BulkProcessingConnector {

        private final BulkProcessingConnector connector;
        private long lastMs;

        private TimedConnector(@NotNull BulkProcessingConnector connector) {
            this.connector = connector;
        }

        @Override
        public MergeResult convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params) {
            long start = System.nanoTime();
            try {
                return connector.convertMergedToPdf(documents, params);
            } finally {
                lastMs = (System.nanoTime() - start) / 1_000_000;
            }
        }

        private long lastMs() {
            return lastMs;
        }
    }
}
