package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Cut empty work item attributes, as Polarion renders a rich text field of the attribute table which is empty, which
 * holds text, and which holds only a picture. The empty one is cut, the other two are kept: a picture is a value though
 * it has no text (#1158).
 */
class CutEmptyWorkItemAttributesTest extends BasePdfConverterTest {

    @Test
    void keepsAnAttributeHoldingOnlyAPicture() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .cutEmptyWIAttributes(true)
                .build();

        byte[] pdf = exportLiveDoc("Work item attributes", readHtmlResource("workItemAttributeWithOnlyAPicture"), params);
        // Compared first, so that the pages are written to the reports whatever fails
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        assertThat(DrawnImages.boxesIn(pdf)).as("The picture of the attribute holding only a picture is printed")
                .anyMatch(image -> image.width() > 100);
        // Named by the sentence above the work items, by their three titles and by the labels of the two attributes kept
        assertThat(occurrences(textOf(pdf), "QA Assessment")).as("The empty attribute is cut, the one with text and the one with a picture are kept")
                .isEqualTo(6);
        assertFalse(differ, "The pages differ from the reference images");
    }

    @SneakyThrows
    private static String textOf(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static int occurrences(String text, String word) {
        int count = 0;
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + word.length())) {
            count++;
        }
        return count;
    }
}
