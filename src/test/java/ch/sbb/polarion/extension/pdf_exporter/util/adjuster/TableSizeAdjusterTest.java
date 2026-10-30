package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import com.helger.css.property.ECSSProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableSizeAdjusterTest {

    @Test
    void bringsATableWiderThanThePageBackToIt() {
        assertEquals("100%", widthOf("<table id='test' style='width: 2000px;'><tr><td>a</td></tr></table>"));
    }

    @Test
    void leavesATableNarrowerThanThePage() {
        assertEquals("300px", widthOf("<table id='test' style='width: 300px;'><tr><td>a</td></tr></table>"));
    }

    @Test
    void leavesATableWhoseWidthIsNoNumber() {
        // The width comes from the document, and one table written wrong must not fail the whole export
        assertEquals("abcpx", widthOf("<table id='test' style='width: abcpx;'><tr><td>a</td></tr></table>"));
    }

    private String widthOf(String html) {
        Document document = Jsoup.parse(html);
        new TableSizeAdjuster(document, ConversionParams.builder().build()).execute();

        String style = document.getElementById("test").attr(HtmlTagAttr.STYLE);
        return CssUtils.getPropertyValue(CssUtils.parseDeclarations(style), ECSSProperty.WIDTH);
    }
}
