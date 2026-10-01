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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tables of real texts, in several languages and scripts and with the values documents hold, laid out as an export lays
 * them out: their text stays inside the page, and the words a column holds are printed whole.
 */
class TableCellTextsTest extends BasePdfConverterTest {

    /** How far text may reach to the right: the page less its right margin. */
    private static final float RIGHT_MARGIN_PT = 40;

    private static Stream<Arguments> tables() {
        return Stream.of(
                // Its words leave the table no room, so its words longer than 15 characters break into parts
                Arguments.of("german", List.of("REQ-1001", "Freigegeben", "Abgelehnt", "In Prüfung", "Personenbezogene", "Aufbewahrungsfrist")),
                Arguments.of("frenchItalian", List.of("anticonstitutionnellement", "precipitevolissimevolmente", "Interopérabilité", "Approuvé", "Approvato")),
                Arguments.of("russian", List.of("Электрооборудование", "Достопримечательности", "Высокопревосходительство", "Согласовано", "Отклонено")),
                Arguments.of("cjk", List.of("REQ-401", "SYS-ABCDEF-123456", "EQP-2026-0001")),
                Arguments.of("rtl", List.of("REQ-501", "EN-50126")),
                Arguments.of("technical", List.of("URL with a query and a fragment", "Windows path", "Java class", "13.9.0-SNAPSHOT,")),
                // Nine columns leave no room either, so its words longer than 10 characters break into parts
                Arguments.of("narrowColumns", List.of("Approved", "2022-01-05", "Priorität", "Testfall", "Betrieb", "13.10.0")),
                Arguments.of("emoji", List.of("Accepted", "Rejected", "Closed"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void laysOutTheTextOfTableCells(@NotNull String table, @NotNull List<String> wholeWords) {
        byte[] pdf = export(readHtmlResource("tableCellTexts/" + table));
        // Compared first, so that the pages are written to the reports whatever fails
        boolean differ = compareContentUsingReferenceImages("tableCellTexts_" + table, pdf);

        assertThat(rightEdge(pdf)).as("The text stays inside the page").isLessThanOrEqualTo(pageWidth(pdf) - RIGHT_MARGIN_PT);
        String text = text(pdf);
        for (String word : wholeWords) {
            assertThat(text).as("\"%s\" is printed whole", word).contains(word);
        }
        assertFalse(differ, "The pages differ from the reference images");
    }

    private byte @NotNull [] export(@NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        return exportLiveDoc("Texts in table cells", content, params);
    }

    /** How far to the right any character of the text reaches on any page, in points. */
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

    /** The text of the document, its lines joined with a space, so that a word split at the end of a line reads as two. */
    @SneakyThrows
    private static @NotNull String text(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }
}
