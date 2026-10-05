package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A landscape page of a document, whose size the merge keeps. */
class LandscapePageMergeTest extends BaseBulkProcessingTest {

    @Test
    void keepsALandscapePageOfADocument() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", page("Alpha", "This page is landscape, as the page break after it asks: the merge keeps the size of each page.")
                        + LANDSCAPE_PAGE_BREAK + page("Alpha", "This page is portrait again.")),
                liveDoc("Bravo", pages("Bravo", "The next document is portrait, as the style package asks."))));

        List<PDRectangle> sizes = pageSizes(result.pdfBytes());
        assertEquals(5, sizes.size(), "Each document is its cover page and its own pages");
        assertTrue(sizes.get(1).getWidth() > sizes.get(1).getHeight(), "The page above the landscape break is landscape");
        assertTrue(sizes.get(2).getWidth() < sizes.get(2).getHeight(), "The page after it is portrait again");
        assertTrue(sizes.get(4).getWidth() < sizes.get(4).getHeight(), "The next document is portrait");
        assertMatchesReferenceImages("bulkProcessingMergeWithALandscapePage", result);
    }
}
