package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Documents WeasyPrint refuses to render, sent to the connector itself, where a document can be made to fail. */
class FailedDocumentMergeTest extends BaseBulkProcessingTest {

    @Test
    void countsADocumentWhichFailsToRenderAndMergesTheOthers() {
        MergeResult result = connector().convertMergedToPdf(List.of(
                rendered("Alpha", "The first document is rendered and merged."),
                failing(),
                rendered("Charlie", "Between Alpha and this document stood one which WeasyPrint refused: it is missing, and the merge counts it as failed.")), startParams());

        assertEquals(1, result.failedDocumentCount());
        List<String> pages = pageTexts(result.pdfBytes());
        assertEquals(2, pages.size());
        assertPage(pages, 0, "The first document is rendered");
        assertPage(pages, 1, "WeasyPrint refused");
        assertMatchesReferenceImages("bulkProcessingMergeWithAFailedDocument", result);
    }

    @Test
    void failsAndDeletesTheJobWhereNoDocumentRenders() {
        BulkProcessingServiceConnector connector = connector();
        // A merge which succeeds keeps its job until the time to live of the service, which shows where the jobs are kept
        int jobsBeforeSuccess = storedJobs();
        connector.convertMergedToPdf(List.of(rendered("Alpha", "Rendered and merged.")), startParams());
        assertEquals(jobsBeforeSuccess + 1, storedJobs(), "A completed job is kept where the jobs are counted");

        int jobsBefore = storedJobs();
        List<MergeDocumentData> documents = List.of(failing(), failing());
        MergeJobStartParams params = startParams();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> connector.convertMergedToPdf(documents, params));

        assertTrue(failure.getMessage().contains("All 2 documents failed to convert"), failure.getMessage());
        assertEquals(jobsBefore, storedJobs(), "The job of the failed merge is deleted, not left for the time to live of the service");
    }
}
