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
    void readsAnAutoHeightAsNoHeight() {
        // The height follows the width there too, so the clamp stretches the image just the same
        assertEquals("contain", objectFitOf("<img id='test' style='width: 592px; height: auto;'/>"));
    }

    @Test
    void leavesAnImageWhichStatesItsOwnHeight() {
        assertEquals("", objectFitOf("<img id='test' style='width: 592px; height: 75px;'/>"));
        assertEquals("", objectFitOf("<img id='test' height='75' style='width: 592px;'/>"));
    }

    @Test
    void leavesAnImageWhichStatesHowItIsDrawn() {
        assertEquals("cover", objectFitOf("<img id='test' style='width: 592px; object-fit: cover;'/>"));
    }

    @Test
    void keepsALimitSmallerThanThePage() {
        Document document = Jsoup.parse("<img id='test' style='max-height: 200px;'/>");
        new ImageSizeAdjuster(document, ConversionParams.builder().build()).execute();

        assertEquals("200px", propertyOf(document, CssProp.MAX_HEIGHT), "A table leaves room for its header this way, and a document may ask for less too");
    }

    @Test
    void bringsALimitLargerThanThePageBackToIt() {
        Document document = Jsoup.parse("<img id='test' style='max-height: 2000px;'/>");
        new ImageSizeAdjuster(document, ConversionParams.builder().build()).execute();

        assertEquals("874px", propertyOf(document, CssProp.MAX_HEIGHT), "Nothing is taller than the page it is printed on");
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
