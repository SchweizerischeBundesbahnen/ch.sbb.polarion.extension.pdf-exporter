package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A merge cancelled while it runs, as an interrupted worker thread cancels it. */
class CancelledMergeTest extends BaseBulkProcessingTest {

    @Test
    void deletesTheJobOfACancelledMerge() {
        Integer jobsBefore = storedJobs();
        BulkProcessingServiceConnector connector = connector();
        List<MergeDocumentData> documents = List.of(rendered("Alpha", "Never rendered."));
        MergeJobStartParams params = startParams();

        Thread.currentThread().interrupt();
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> connector.convertMergedToPdf(documents, params));

        assertTrue(failure.getMessage().contains("was cancelled"), failure.getMessage());
        assertFalse(Thread.currentThread().isInterrupted(), "The flag is cleared, so that a reused worker thread is not cancelled too");
        assertStoredJobs(jobsBefore, 0, "The job of the cancelled merge is deleted");
    }
}
