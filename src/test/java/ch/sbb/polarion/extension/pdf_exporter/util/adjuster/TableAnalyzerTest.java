package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableAnalyzerTest {

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
        assertEquals(4, columnWidths.size());
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
        assertEquals(3, columnWidths.size());
        assertEquals(121, columnWidths.get(0));
        assertEquals(566, columnWidths.get(1));
        assertEquals(242, columnWidths.get(2));
    }

}
