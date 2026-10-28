package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.TableAnalyzer;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tables as wide as the room a page leaves them, laid out as an export lays them out. The measure gives the words of a
 * table places to break only where it is wider than the page leaves it: the paper less the margins of its {@code @page}
 * rule, and less the margins and paddings of the body.
 */
class TableWidthOfThePageTest extends BasePdfConverterTest {

    /** A word which keeps whole, as it is not longer than a word which gets places to break by itself. */
    private static final String WORD = "Zulassungsverfahren";

    private static final String CELL_STYLE = "font-size:9pt;line-height:1.5;text-align:left;vertical-align:top;width:auto;border:1px solid #CCCCCC;padding:5px;";

    /** The side margin the default CSS gives every page, 60px, in points. */
    private static final float DEFAULT_MARGIN_PT = 45;

    /** What the body of the default CSS takes on the right beside its content, a margin of 8px and a padding of 10px, in points. */
    private static final float BODY_RIGHT_PT = 13.5f;

    /** The top margin of the default CSS, 120px, in points, which holds the header: it reaches to the side margins. */
    private static final float TOP_MARGIN_PT = 90;

    /** The bottom margin of the default CSS, 90px, in points, which holds the footer. */
    private static final float BOTTOM_MARGIN_PT = 67.5f;

    /** A portrait A4 leaves a table 627px, which the approximated width of 592px and its 6 % allowance came close to. */
    @Test
    void keepsTheWordsOfATableWhichFitsAPortraitA4Whole() {
        String table = tableMeasuring(600, 620);

        byte[] pdf = export(table, PaperSize.A4, Orientation.PORTRAIT);
        boolean differ = compareContentUsingReferenceImages("tableWidthOfThePage_a4Portrait", pdf);

        assertThat(rightEdge(pdf)).as("The table stays inside the body").isLessThanOrEqualTo(pageWidth(pdf) - DEFAULT_MARGIN_PT - BODY_RIGHT_PT);
        assertThat(wholeWords(pdf)).as("Every word prints whole").isEqualTo(cellsOf(table));
        assertFalse(differ, "The pages differ from the reference images");
    }

    /** A portrait A3 leaves a table 956px: the approximated width of 835px and its allowance broke the words of a table up to that. */
    @Test
    void keepsTheWordsOfATableWhichFitsAPortraitA3Whole() {
        String table = tableMeasuring(900, 945);

        byte[] pdf = export(table, PaperSize.A3, Orientation.PORTRAIT);
        boolean differ = compareContentUsingReferenceImages("tableWidthOfThePage_a3Portrait", pdf);

        assertThat(rightEdge(pdf)).as("The table stays inside the body").isLessThanOrEqualTo(pageWidth(pdf) - DEFAULT_MARGIN_PT - BODY_RIGHT_PT);
        assertThat(wholeWords(pdf)).as("Every word prints whole").isEqualTo(cellsOf(table));
        assertFalse(differ, "The pages differ from the reference images");
    }

    /** A landscape letter leaves a table 890px: the approximated width of 900px and its allowance let a table reach past the page. */
    @Test
    void breaksTheWordsOfATableTooWideForALandscapeLetter() {
        String table = tableMeasuring(930, 950);

        byte[] pdf = export(table, PaperSize.LETTER, Orientation.LANDSCAPE);
        boolean differ = compareContentUsingReferenceImages("tableWidthOfThePage_letterLandscape", pdf);

        assertThat(rightEdge(pdf)).as("The table stays inside the body").isLessThanOrEqualTo(pageWidth(pdf) - DEFAULT_MARGIN_PT - BODY_RIGHT_PT);
        assertThat(wholeWords(pdf)).as("Words break to fit").isLessThan(cellsOf(table));
        assertFalse(differ, "The pages differ from the reference images");
    }

    /** The margins of a page the CSS of an export states leave a table less room, which the approximated width did not know. */
    @Test
    void breaksTheWordsOfATableTooWideForTheMarginsTheCssStates() {
        // 200px on each side leave a portrait A4 a table 793 - 400 - 26 = 367px
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(readFontCss() + "@page { margin: 120px 200px 90px 200px; }")
                .build());
        String table = tableMeasuring(450, 550);

        byte[] pdf = export(table, PaperSize.A4, Orientation.PORTRAIT);
        boolean differ = compareContentUsingReferenceImages("tableWidthOfThePage_cssMargins", pdf);

        assertThat(rightEdge(pdf)).as("The table stays inside the body").isLessThanOrEqualTo(pageWidth(pdf) - 150 - BODY_RIGHT_PT);
        assertThat(wholeWords(pdf)).as("Words break to fit").isLessThan(cellsOf(table));
        assertFalse(differ, "The pages differ from the reference images");
    }

    /**
     * A table of {@link #WORD} in each cell, whose narrowest layout the measure finds between the given widths: as many
     * columns as it takes, the last one of a shorter part of the word to come between them.
     */
    private static @NotNull String tableMeasuring(int narrowest, int widest) {
        for (int columns = 1; columns < 20; columns++) {
            for (int length = WORD.length(); length > 3; length--) {
                String table = table(columns, length);
                int width = TableAnalyzer.analyze(Jsoup.parse(table).selectFirst("table"), 1).tableWidth();
                if (width > narrowest && width < widest) {
                    return table;
                }
            }
        }
        throw new IllegalStateException("No table of " + WORD + " measures between " + narrowest + " and " + widest + " px");
    }

    private static @NotNull String table(int columns, int lastLength) {
        StringBuilder head = new StringBuilder();
        StringBuilder row = new StringBuilder();
        for (int column = 0; column < columns; column++) {
            head.append("<th style=\"font-weight:bold;").append(CELL_STYLE).append("\">Column ").append(column + 1).append("</th>");
            String word = column == columns - 1 ? WORD.substring(0, lastLength) : WORD;
            row.append("<td style=\"").append(CELL_STYLE).append("\">").append(word).append("</td>");
        }
        return "<table class=\"polarion-Document-table\" style=\"width:100%;border:1px solid #CCCCCC;border-collapse:collapse;\">"
                + "<thead><tr>" + head + "</tr></thead><tbody><tr>" + row + "</tr></tbody></table>";
    }

    /** The cells of the table which hold the whole word. */
    private static int cellsOf(@NotNull String table) {
        return Jsoup.parse(table).select("td:containsOwn(" + WORD + ")").size();
    }

    /** How many times the PDF prints the whole word. */
    @SneakyThrows
    private static int wholeWords(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            return text.split(WORD, -1).length - 1;
        }
    }

    private byte @NotNull [] export(@NotNull String content, @NotNull PaperSize paperSize, @NotNull Orientation orientation) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(orientation)
                .paperSize(paperSize)
                .build();
        return exportLiveDoc("Table as wide as the page", content, params);
    }

    /** How far to the right any character of the content reaches on any page, in points: the header and the footer left out. */
    @SneakyThrows
    private static float rightEdge(byte @NotNull [] pdf) {
        float[] edge = {0};
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (TextPosition position : positions) {
                        float y = position.getYDirAdj();
                        if (y < TOP_MARGIN_PT || y > position.getPageHeight() - BOTTOM_MARGIN_PT) {
                            continue;
                        }
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
}
