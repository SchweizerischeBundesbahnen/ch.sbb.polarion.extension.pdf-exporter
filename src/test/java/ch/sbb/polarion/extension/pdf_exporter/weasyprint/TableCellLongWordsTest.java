package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * The words of a table cell break only where it is long: a URL or an ID at its parts, a short word never.
 */
class TableCellLongWordsTest extends BasePdfConverterTest {

    /** How far text may reach to the right: the page less its right margin. */
    private static final float RIGHT_MARGIN_PT = 40;

    /** How long an export of a few cells of long text may take: an order of magnitude more than it does. */
    private static final Duration LONG_CELLS_LIMIT = Duration.ofSeconds(20);

    @Test
    void keepsLongWordsInsideThePageAndShortWordsWhole() {
        String longWords = "<table><tr>"
                + "<td style=\"border: 1px solid black;\">very_long_word_very_long_word_very_long_word_very_long_word_very_long_word_very_long_word</td>".repeat(5)
                + "</tr></table>";
        // Unicode allows no break before a slash, even after a space, so this is one long word too
        String slashes = "<table><tr>" + ("<td style=\"border: 1px solid black;\">" + "/-123 ".repeat(30) + "</td>").repeat(3) + "</tr></table>";
        String narrowColumn = "<table class=\"polarion-Document-table\" style=\"width: 100%; border-collapse: collapse;\"><tr><th>TERM</th><th>DEFINITION</th></tr>"
                + "<tr><td>Catalog</td><td>The database of eBooks available for loan and possibly for sale, described at length so that this column takes most of the page.</td></tr>"
                + "<tr><td>Disapproved</td><td>TMSPRG-13164 2022-01-05</td></tr></table>";

        byte[] pdf = export(longWords + slashes + narrowColumn);

        assertThat(rightEdge(pdf)).as("The long words and the runs of slashes stay inside the page").isLessThanOrEqualTo(pageWidth(pdf) - RIGHT_MARGIN_PT);
        assertThat(text(pdf)).as("A short word of a narrow column is not split").contains("Disapproved").contains("Catalog");
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    void exportsCellsOfLongTextInSeconds() {
        String sentences = IntStream.rangeClosed(1, 50).mapToObj(sentence -> "Sentence " + sentence + " of a description which takes a lot of room.")
                .collect(Collectors.joining(" "));
        String table = "<table style=\"border-collapse: collapse;\"><tr><th>ID</th><th>Description</th></tr>"
                + IntStream.rangeClosed(1, 3).mapToObj(row -> "<tr><td>REQ-" + row + "</td><td>" + sentences + "</td></tr>").collect(Collectors.joining())
                + "</table>";

        long start = System.nanoTime();
        export(table);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).as("Each character of a cell is not a place to break, so the cells are laid out quickly")
                .isLessThan(LONG_CELLS_LIMIT);
    }

    private byte @NotNull [] export(@NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Long words in table cells")
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        return converter.convertToPdf(params, null);
    }

    /** How far to the right any character of the text reaches, in points. */
    @SneakyThrows
    private static float rightEdge(byte @NotNull [] pdf) {
        float[] edge = {0};
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (TextPosition position : positions) {
                        edge[0] = Math.max(edge[0], position.getXDirAdj() + position.getWidthDirAdj());
                    }
                }
            };
            stripper.getText(document);
        }
        return edge[0];
    }

    @SneakyThrows
    private static float pageWidth(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(0).getMediaBox().getWidth();
        }
    }

    @SneakyThrows
    private static @NotNull String text(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
