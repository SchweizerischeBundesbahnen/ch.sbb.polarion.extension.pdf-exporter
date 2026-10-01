package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableRowsAdjusterTest {

    private static final ConversionParams A4_PORTRAIT = ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build();

    /** Text enough for a cell far taller than a page. */
    private static final String TALLER_THAN_A_PAGE = "A sentence of a cell which no page can hold whole. ".repeat(400);

    @Test
    void keepsTheRowsWhichFitAPageWhole() {
        Document document = Jsoup.parse("""
                <table><thead><tr><th>Account type</th><th>Permissions</th></tr></thead>
                <tbody><tr><td>Patron</td><td>Can access the general library features.</td></tr>
                <tr><td>Student</td><td>Same as a Patron.</td></tr></tbody></table>""");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.select("tr").eachAttr("style")).containsOnly("break-inside:avoid;").hasSize(3);
    }

    @Test
    void leavesARowTallerThanAPageFreeToBreak() {
        Document document = Jsoup.parse("<table><tbody><tr><td>Short</td></tr><tr><td>" + TALLER_THAN_A_PAGE + "</td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        List<Element> rows = document.select("tr");
        assertThat(rows.get(0).attr("style")).isEqualTo("break-inside:avoid;");
        assertThat(rows.get(1).hasAttr("style")).as("A row taller than a page would take a page of its own").isFalse();
    }

    @Test
    void leavesARowOfHalfAPageFreeToBreak() {
        // Kept whole, it would move to the next page and leave up to half of this one empty
        Document document = Jsoup.parse("<table><tbody><tr><td>" + "A line of a cell.<br/>".repeat(25) + "</td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.selectFirst("tr").hasAttr("style")).isFalse();
    }

    @Test
    void leavesARowWithAnImageToTheImageAdjusters() {
        // The image is an address the measure cannot load yet, so the height of its row is not known
        Document document = Jsoup.parse("<table><tbody><tr><td><img src=\"/polarion/diagram.png\"/></td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.selectFirst("tr").hasAttr("style")).isFalse();
    }

    @Test
    void keepsARowWithAnIconWhole() {
        Document document = Jsoup.parse("""
                <table><tbody><tr><td>Status</td><td><span class="polarion-JSEnumOption" title="Draft">\
                <img src="/polarion/icons/default/enums/req_status_draft.gif"/>Draft</span></td></tr></tbody></table>""");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.selectFirst("tr").attr("style")).isEqualTo("break-inside:avoid;");
    }

    @Test
    void keepsARowWithAnIconMarkedBeforeItWasEmbeddedWhole() {
        Document document = Jsoup.parse("<table><tbody><tr><td><img src=\"/polarion/ria/images/types/requirement.gif\"/>Patron</td></tr></tbody></table>");
        TableRowsAdjuster.markIcons(document);
        Element icon = document.selectFirst("img");
        icon.attr("src", "data:image/gif;base64,R0lGODlhAQABAAAAACw=");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.selectFirst("tr").attr("style")).isEqualTo("break-inside:avoid;");
        assertThat(icon.hasAttr("data-pdf-exporter-icon")).as("The mark is gone once the rows are measured").isFalse();
    }

    @Test
    void leavesTheAttributesOfAnIconAsTheDocumentStatesThem() {
        Document document = Jsoup.parse("<table><tbody><tr><td><img class=\"polarion-Icons\" data-icon=\"status\" src=\"/polarion/icons/draft.gif\"/>Draft</td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.selectFirst("img").attr("data-icon")).isEqualTo("status");
    }

    @Test
    void leavesARowWhichStatesHowItBreaksAsItIs() {
        Document document = Jsoup.parse("""
                <table><tbody><tr style="break-inside: auto"><td>Stated</td></tr>
                <tr style="page-break-inside: auto"><td>Stated the old way</td></tr></tbody></table>""");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.select("tr").eachAttr("style")).containsExactly("break-inside: auto", "page-break-inside: auto");
    }

    @Test
    void keepsTheShortRowsOfANestedTableInARowTooTallToKeep() {
        // The row around is free to break, so the short rows of the table in it are kept whole on their own
        Document document = Jsoup.parse("<table><tbody><tr><td>" + TALLER_THAN_A_PAGE
                + "<table><tbody><tr><td>Patron</td><td>Can access the library.</td></tr></tbody></table></td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        List<Element> rows = document.select("tr");
        assertThat(rows.get(0).hasAttr("style")).as("The row around is taller than a page").isFalse();
        assertThat(rows.get(1).attr("style")).isEqualTo("break-inside:avoid;");
    }

    @Test
    void measuresANestedTableAtTheWidthOfItsCell() {
        // Forty words make two lines across the page, but some forty lines in a column of 60px
        String words = "word ".repeat(40);
        Document document = Jsoup.parse("<table><tbody><tr><td style=\"width: 60px\"><table><tbody><tr><td>" + words
                + "</td></tr></tbody></table></td><td>" + "A cell which takes the rest of the width. ".repeat(10) + "</td></tr></tbody></table>");

        new TableRowsAdjuster(document, A4_PORTRAIT).execute();

        assertThat(document.select("tr").get(1).hasAttr("style")).as("Measured in its cell, the inner row is far taller than a quarter of a page").isFalse();
    }

    @Test
    void measuresARowInTheFontItInheritsFromAroundTheTable() {
        String table = "<table><tbody><tr><td>" + "A line of a cell.<br/>".repeat(6) + "</td></tr></tbody></table>";
        Document plain = Jsoup.parse(table);
        Document large = Jsoup.parse("<div style=\"font-size: 40px; line-height: 1.5\">" + table + "</div>");

        new TableRowsAdjuster(plain, A4_PORTRAIT).execute();
        new TableRowsAdjuster(large, A4_PORTRAIT).execute();

        assertThat(plain.selectFirst("tr").attr("style")).as("Six lines of the default font are a short row").isEqualTo("break-inside:avoid;");
        assertThat(large.selectFirst("tr").hasAttr("style")).as("Six lines of a font of 40px are more than a quarter of a page").isFalse();
    }

    @Test
    void measuresEveryRowOfTheTableInOrder() {
        Element table = Jsoup.parse("""
                <table><thead><tr><th>Head</th></tr></thead>
                <tbody><tr><td>One line</td></tr><tr><td>Two<br/>lines</td></tr></tbody></table>""").selectFirst("table");

        List<Integer> heights = TableAnalyzer.analyze(table, 600).rowHeights();

        assertThat(heights).hasSize(3);
        assertThat(heights.get(2)).as("Two lines take more height than one").isGreaterThan(heights.get(1));
    }
}
