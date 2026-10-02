package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The attribute table of a work item and the links to work items it holds, as Polarion renders them.
 * <p>
 * Its labels are bold, as in Polarion. A link to a work item, as "DGT-1458 - Work Item with Attributes", stays together
 * in the attribute table and in a narrow column of a document table: the icon on the line of the ID, the ID whole on
 * one line, the dash after it on that line, and a suspect icon before the icon of the work item. Only the title, and
 * the revision after it, may wrap. A column narrower than a link widens to hold it rather than split it.
 * </p>
 */
class WorkItemAttributesTest extends BasePdfConverterTest {

    /** The first word of each label: a label may wrap in its column, as "Linked Work Items" does. */
    private static final List<String> LABELS = List.of("Severity", "Linked", "Author", "Created");

    private static final List<String> LINKED_IDS = List.of(
            "DGT-1458", "DGT-1462", "DGT-1470", "SYSTEMREQUIREMENTS-123456", "EL-101", "EL-757", "EL-759", "EL-760", "EL-761",
            "REQ-200", "REQ-201", "REQ-202", "TC-300", "TC-301", "TC-302", "FEAT-40", "FEAT-41", "FEAT-42", "DGT-1600", "DGT-1601", "DGT-1602");

    /** How far the middle of an icon may be from the middle of its ID, in points. */
    private static final float SAME_LINE_PT = 3;

    /** How far the ID may start after the right edge of its icon, in points. */
    private static final float BESIDE_PT = 4;

    /** The links marked suspect, whose suspect icon stands before the icon of the work item. */
    private static final List<String> SUSPECT_IDS = List.of("EL-101", "EL-760", "EL-761", "REQ-200", "REQ-201", "REQ-202");

    /** A word printed on one line: where it starts, on which page, and the text which follows it on that line. */
    private record Word(int page, @NotNull TextPosition first, @NotNull String rest) {
    }

    @Test
    void printsTheLabelsOfTheAttributeTableBold() {
        byte[] pdf = export();
        // Compared first, so that the pages are written to the reports whatever fails
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        Map<String, Word> labels = wordsIn(pdf, LABELS);
        for (String label : LABELS) {
            assertThat(labels.get(label)).as("The label \"%s\" is printed", label).isNotNull();
            assertThat(labels.get(label).first().getFont().getName()).as("The label \"%s\" is bold", label).containsIgnoringCase("bold");
        }
        assertFalse(differ, "The pages differ from the reference images");
    }

    @Test
    void keepsTheIconTheIdAndTheDashOfALinkTogether() {
        byte[] pdf = export();

        Map<String, Word> ids = wordsIn(pdf, LINKED_IDS);
        List<DrawnImages.Box> icons = DrawnImages.boxesIn(pdf);
        for (String id : LINKED_IDS) {
            Word word = ids.get(id);
            assertThat(word).as("The ID %s is printed whole on one line", id).isNotNull();
            assertThat(word.rest().stripLeading()).as("The dash follows the ID %s on its line", id).startsWith("-");

            TextPosition first = word.first();
            float middle = first.getYDirAdj() - first.getHeightDir() / 2;
            DrawnImages.Box typeIcon = iconBefore(icons, word.page(), first.getXDirAdj(), middle);
            assertThat(typeIcon).as("The icon of %s stands right before it, on its line", id).isNotNull();
            if (SUSPECT_IDS.contains(id)) {
                assertThat(iconBefore(icons, word.page(), typeIcon.left(), middle)).as("The suspect icon of %s stands right before its icon, on its line", id).isNotNull();
            }
        }
    }

    /** The icon which ends right before the given place, on the line whose middle is given, if any. */
    private static DrawnImages.Box iconBefore(@NotNull List<DrawnImages.Box> icons, int page, float left, float middle) {
        return icons.stream()
                .filter(icon -> icon.page() == page && Math.abs(icon.middle() - middle) <= SAME_LINE_PT)
                .filter(icon -> left - (icon.left() + icon.width()) >= -0.5f && left - (icon.left() + icon.width()) <= BESIDE_PT)
                .findFirst()
                .orElse(null);
    }

    private byte @NotNull [] export() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .fitToPage(true)
                .build();
        return exportLiveDoc("Work item attributes", readHtmlResource("workItemAttributes"), params);
    }

    /** Each of the words found whole on one line, with the text which follows it there. */
    @SneakyThrows
    private static @NotNull Map<String, Word> wordsIn(byte @NotNull [] pdf, @NotNull List<String> words) {
        Map<String, Word> found = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (String word : words) {
                        int at = text.indexOf(word);
                        if (at >= 0 && !found.containsKey(word)) {
                            found.put(word, new Word(getCurrentPageNo() - 1, positions.get(at), text.substring(at + word.length())));
                        }
                    }
                }
            };
            stripper.getText(document);
        }
        return found;
    }
}
