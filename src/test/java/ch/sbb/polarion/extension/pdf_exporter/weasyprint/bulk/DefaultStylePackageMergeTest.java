package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The happy path: documents exported with the default style package, merged in the order they are added. */
class DefaultStylePackageMergeTest extends BaseBulkProcessingTest {

    @Test
    void mergesDocumentsWithTheDefaultStylePackage() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", pages("Alpha",
                        "The first document of the merge. The documents follow each other in the order they are added: Alpha, Bravo, then Charlie.",
                        "The second page of Alpha: a page break inside a document is kept.")),
                liveDoc("Bravo", pages("Bravo",
                        "The second document, of one page. Its cover page counts 2 pages: those of Bravo, not those of the whole merge.")),
                liveDoc("Charlie", pages("Charlie",
                        "The third and last document, of three pages.",
                        "The second page of Charlie.",
                        "The third page of Charlie, the last of the merge."))));

        assertEquals(0, result.failedDocumentCount());
        List<String> pages = pageTexts(result.pdfBytes());
        assertEquals(9, pages.size(), "Each document is its cover page and its own pages");
        assertPage(pages, 0, "Alpha", "page 1 of 3");
        assertPage(pages, 1, "The first document of the merge");
        assertPage(pages, 2, "The second page of Alpha");
        assertPage(pages, 3, "Bravo", "page 1 of 2");
        assertPage(pages, 4, "The second document");
        assertPage(pages, 5, "Charlie", "page 1 of 4");
        assertPage(pages, 6, "The third and last document");
        assertPage(pages, 8, "the last of the merge");
        assertMatchesReferenceImages("bulkProcessingMergeWithDefaultStylePackage", result);
    }
}
