package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A merge of one document, which is as its export alone would be. */
class SingleDocumentMergeTest extends BaseBulkProcessingTest {

    @Test
    void mergesASingleDocument() {
        MergeResult result = converter.convertMergedToPdf(List.of(liveDoc("Alpha", pages("Alpha",
                "The only document of the merge: its cover page and this page are the whole PDF, as an export of Alpha alone would be."))));

        List<String> pages = pageTexts(result.pdfBytes());
        assertEquals(2, pages.size());
        assertPage(pages, 0, "Alpha", "page 1 of 2");
        assertPage(pages, 1, "The only document of the merge");
        assertMatchesReferenceImages("bulkProcessingMergeOfASingleDocument", result);
    }
}
