package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Letters beyond ASCII, in the titles and in the text, which the merge prints as they are. */
class ScriptsMergeTest extends BaseBulkProcessingTest {

    @Test
    void keepsTextOfAnyScript() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Ärger", pages("Ärger", "German umlauts and ß, in the title and in the text: Grüße aus Zürich, Fußgängerübergänge, Äpfel und Öl. Every letter is printed, none is a box.")),
                liveDoc("Требования", pages("Требования", "Cyrillic, in the title and in the text: Требования к подвижному составу. Every letter is printed, none is a box."))));

        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 0, "Ärger");
        assertPage(pages, 1, "Grüße aus Zürich");
        assertPage(pages, 2, "Требования");
        assertPage(pages, 3, "Требования к подвижному составу");
        assertMatchesReferenceImages("bulkProcessingMergeOfAnyScript", result);
    }
}
