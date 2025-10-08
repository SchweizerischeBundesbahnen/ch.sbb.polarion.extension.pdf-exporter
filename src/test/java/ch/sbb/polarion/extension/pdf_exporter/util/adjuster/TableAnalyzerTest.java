package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableAnalyzerTest {

    @Test
    void getColumnWidthsA5PortraitTest() {
        Element table = new Element(Tag.valueOf("table"), "");

        Element row1 = table.appendElement("tr");
        row1.appendElement("td").text("Small image:");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/150x100")
                .attr("width", "150")
                .attr("height", "100");
        row1.appendElement("td").text("Description text here");

        Element row2 = table.appendElement("tr");
        row2.appendElement("td").text("Large image:");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/600x400")
                .attr("width", "600")
                .attr("height", "400");
        row2.appendElement("td").text("This is a much larger image that might overflow");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().build()));

        // Should have 3 columns total
        assertEquals(3, columnWidths.size());

        // Columns width calculation is not absolutely accurate and can differ from system to system, so we check gracefully
        assertTrue(columnWidths.get(0) > 29 && columnWidths.get(0) < 39);
        assertTrue(columnWidths.get(1) > 489 && columnWidths.get(1) < 499);
        assertTrue(columnWidths.get(2) > 58 && columnWidths.get(2) < 68);
    }

    @Test
    void getColumnWidthsA5LandscapeTest() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element tbody = table.appendElement("tbody");

        Element row1 = tbody.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/100x100")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/120x80")
                .attr("width", "120")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/150x100")
                .attr("width", "150")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/100x120")
                .attr("width", "100")
                .attr("height", "120");

        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/200x150")
                .attr("width", "200")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/250x200")
                .attr("width", "250")
                .attr("height", "200");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/300x200")
                .attr("width", "300")
                .attr("height", "200");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/180x180")
                .attr("width", "180")
                .attr("height", "180");

        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/400x300")
                .attr("width", "400")
                .attr("height", "300");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/500x350")
                .attr("width", "500")
                .attr("height", "350");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/450x300")
                .attr("width", "450")
                .attr("height", "300");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/350x400")
                .attr("width", "350")
                .attr("height", "400");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().orientation(Orientation.LANDSCAPE).build()));

        // Should have 4 columns total
        assertEquals(4, columnWidths.size());

        // Columns width calculation is not absolutely accurate and can differ from system to system, so we check gracefully
        assertTrue(columnWidths.get(0) > 213 && columnWidths.get(0) < 223);
        assertTrue(columnWidths.get(1) > 268 && columnWidths.get(1) < 278);
        assertTrue(columnWidths.get(2) > 241 && columnWidths.get(2) < 251);
        assertTrue(columnWidths.get(3) > 186 && columnWidths.get(3) < 196);

        assertEquals(218, columnWidths.get(0));
        assertEquals(273, columnWidths.get(1));
        assertEquals(246, columnWidths.get(2));
        assertEquals(191, columnWidths.get(3));
    }

    @Test
    void getColumnWidthsWithHeadersTest() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element tbody = table.appendElement("tbody");

        Element header = tbody.appendElement("tr");
        header.appendElement("th").appendText("Column 1");
        header.appendElement("th").appendText("Column 2");
        header.appendElement("th").appendText("Column 3");

        Element row1 = tbody.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/100x100")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/700x300")
                .attr("width", "700")
                .attr("height", "300");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/250x200")
                .attr("width", "250")
                .attr("height", "200");

        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td").text("Small&nbsp;text");
        row2.appendElement("td").text("This column has a very long text description instead of an image, which should make the analyzer work differently");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/300x300")
                .attr("width", "300")
                .attr("height", "300");

        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/150x100")
                .attr("width", "150")
                .attr("height", "100");
        row3.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/200x150")
                .attr("width", "200")
                .attr("height", "150");
        row3.appendElement("td").text("Short");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().orientation(Orientation.LANDSCAPE).build()));

        // Should have 3 columns total
        assertEquals(3, columnWidths.size());

        // Columns width calculation is not absolutely accurate and can differ from system to system, so we check gracefully
        assertTrue(columnWidths.get(0) > 115 && columnWidths.get(0) < 125);
        assertTrue(columnWidths.get(1) > 561 && columnWidths.get(1) < 571);
        assertTrue(columnWidths.get(2) > 237 && columnWidths.get(2) < 247);
    }

    @Test
    void getColumnWidthsWhenNoExplicitValuesTest() {
        Element table = new Element(Tag.valueOf("table"), "");

        Element row1 = table.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/100x100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/300x200");
        row1.appendElement("td").text("Text content");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/500x300");

        // Row 2
        Element row2 = table.appendElement("tr");
        row2.appendElement("td").text("A");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/400x250");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/150x150");
        row2.appendElement("td").text("Very long text that should influence column width significantly");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().build()));

        // Should have 4 columns total
        assertEquals(4, columnWidths.size());

        // Columns width calculation is not absolutely accurate and can differ from system to system, so we check gracefully
        assertTrue(columnWidths.get(0) > 9 && columnWidths.get(0) < 19);
        assertTrue(columnWidths.get(1) > 6 && columnWidths.get(1) < 16);
        assertTrue(columnWidths.get(2) > 88 && columnWidths.get(2) < 98);
        assertTrue(columnWidths.get(3) > 467 && columnWidths.get(3) < 477);
    }

    @Test
    void getColumnWidthsWithColspanTest() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element tbody = table.appendElement("tbody");

        // First row: 4 regular columns
        Element row1 = tbody.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/100x100")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/150x100")
                .attr("width", "250")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/120x100")
                .attr("width", "120")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/130x100")
                .attr("width", "330")
                .attr("height", "100");

        // Second row: one cell with colspan=2, then two regular cells
        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td")
                .attr("colspan", "2")
                .appendElement("img")
                .attr("src", "https://via.placeholder.com/300x150")
                .attr("width", "300")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/200x150")
                .attr("width", "200")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "https://via.placeholder.com/180x150")
                .attr("width", "180")
                .attr("height", "150");

        // Third row: regular cell, colspan=2, then regular cell
        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").text("Text");
        row3.appendElement("td")
                .attr("colspan", "2")
                .text("Spanning two columns");
        row3.appendElement("td").text("Last");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().build()));

        // Should have 4 columns total
        assertEquals(4, columnWidths.size());

        // Columns width calculation is not absolutely accurate and can differ from system to system, so we check gracefully
        assertTrue(columnWidths.get(0) > 101 && columnWidths.get(0) < 111);
        assertTrue(columnWidths.get(1) > 145 && columnWidths.get(1) < 155);
        assertTrue(columnWidths.get(2) > 131 && columnWidths.get(2) < 141);
        assertTrue(columnWidths.get(3) > 193 && columnWidths.get(3) < 203);
    }

}
