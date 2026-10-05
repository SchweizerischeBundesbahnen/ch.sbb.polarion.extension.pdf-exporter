package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A document without content, which is merged all the same. */
class DocumentWithoutContentMergeTest extends BaseBulkProcessingTest {

    @Test
    void mergesADocumentWithoutContent() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", ""),
                liveDoc("Bravo", pages("Bravo", "The document before this one, Alpha, has no content: it is its cover page and one empty page, and it is merged all the same."))));

        assertEquals(0, result.failedDocumentCount());
        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 0, "Alpha", "page 1 of 2");
        assertPage(pages, pages.size() - 1, "has no content");
        assertMatchesReferenceImages("bulkProcessingMergeWithADocumentWithoutContent", result);
    }
}
