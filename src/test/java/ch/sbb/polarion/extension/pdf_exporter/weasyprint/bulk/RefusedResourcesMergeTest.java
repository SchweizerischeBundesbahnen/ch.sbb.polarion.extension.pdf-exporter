package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Resources at internal addresses, which the default policy refuses before any request, so the merge leaves them out. */
class RefusedResourcesMergeTest extends BaseBulkProcessingTest {

    @Test
    void mergesADocumentWhoseResourcesThePolicyRefuses() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", "<h2>Alpha</h2>"
                        + "<p>An image embedded in the document is printed:</p>"
                        + "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='40'><rect width='200' height='40' fill='#2e7d32'/>"
                        + "<text x='12' y='26' fill='#ffffff' font-size='16' font-family='Arial'>An embedded image</text></svg>"
                        + "<p>Between the two rules below stood two images and a background from internal addresses: loopback (127.0.0.1), "
                        + "the metadata of a cloud (169.254.169.254) and a private network (10.0.0.1). The default policy refuses them, so nothing is printed there.</p>"
                        + "<hr/>"
                        + "<p><img src='http://127.0.0.1:9/logo.png' alt='loopback'/></p>"
                        + "<p><img src='http://169.254.169.254/latest/meta-data/' alt='metadata'/></p>"
                        + "<div style='background-image: url(http://10.0.0.1/background.png); height: 50px;'></div>"
                        + "<hr/>"
                        + "<p>The document is exported all the same.</p>"),
                liveDoc("Bravo", pages("Bravo", "The next document is merged as usual."))));

        assertEquals(0, result.failedDocumentCount(), "A refused resource is left out, the document is still exported");
        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 1, "nothing is printed there", "exported all the same");
        assertPage(pages, 3, "merged as usual");
        assertMatchesReferenceImages("bulkProcessingMergeWithRefusedResources", result);
    }
}
