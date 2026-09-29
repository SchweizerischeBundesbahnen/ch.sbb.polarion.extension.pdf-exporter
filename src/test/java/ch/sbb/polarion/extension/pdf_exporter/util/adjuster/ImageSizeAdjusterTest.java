package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageSizeAdjusterTest {

    @Test
    void keepsTheRatioOfAnImageWhichStatesNoHeight() {
        // A height clamp on such an image shortens the height alone, which stretches the drawing
        assertEquals("contain", objectFitOf("<img id='test' style='width: 592px;'/>"));
        assertEquals("contain", objectFitOf("<img id='test' style='max-width: 650px;'/>"));
    }

    @Test
    void leavesAnImageWhichStatesItsOwnHeight() {
        assertEquals("", objectFitOf("<img id='test' style='width: 592px; height: 75px;'/>"));
        assertEquals("", objectFitOf("<img id='test' height='75' style='width: 592px;'/>"));
    }

    @Test
    void restrictsTheHeightOfEveryImage() {
        Document document = Jsoup.parse("<img id='test' style='width: 592px;'/>");
        new ImageSizeAdjuster(document, ConversionParams.builder().build()).execute();

        assertEquals("874px", propertyOf(document, CssProp.MAX_HEIGHT));
    }

    private String objectFitOf(String html) {
        Document document = Jsoup.parse(html);
        new ImageSizeAdjuster(document, ConversionParams.builder().build()).execute();

        return propertyOf(document, CssProp.OBJECT_FIT);
    }

    private String propertyOf(Document document, String property) {
        String style = document.getElementById("test").attr(HtmlTagAttr.STYLE);
        return CssUtils.getPropertyValue(CssUtils.parseDeclarations(style), property);
    }
}
