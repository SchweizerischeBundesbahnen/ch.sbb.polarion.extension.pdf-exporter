package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.helger.css.property.ECSSProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableSizeAdjusterTest {

    private static final ConversionParams A4_PORTRAIT = ConversionParams.builder().build();

    @Test
    void bringsATableWiderThanThePageBackToIt() {
        assertEquals("100%", widthOf("width: 2000px;", A4_PORTRAIT));
    }

    @Test
    void leavesATableNarrowerThanThePage() {
        assertEquals("300px", widthOf("width: 300px;", A4_PORTRAIT));
    }

    @ParameterizedTest
    @MethodSource("pageFormats")
    void measuresAWidthInPixelsAgainstTheWidthOfThePage(PaperSize paperSize, Orientation orientation) {
        ConversionParams conversionParams = ConversionParams.builder().paperSize(paperSize).orientation(orientation).build();
        int pageWidth = PaperSizeUtils.getMaxWidth(conversionParams);

        assertEquals(pageWidth + "px", widthOf("width: " + pageWidth + "px;", conversionParams), "A table as wide as the page fits on it");
        assertEquals("100%", widthOf("width: " + (pageWidth + 1) + "px;", conversionParams));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5%", "50%", "99.9%", "100%"})
    void leavesATableWhichTakesNoMoreThanTheWholeWidth(String width) {
        // A percentage is of the width the table sits in, so up to 100% the table fits in it
        assertEquals(width, widthOf("width: " + width + ";", A4_PORTRAIT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.1%", "140.45%", "200%"})
    void bringsATableWhichTakesMoreThanTheWholeWidthBackToIt(String width) {
        assertEquals("100%", widthOf("width: " + width + ";", A4_PORTRAIT));
    }

    @ParameterizedTest
    @MethodSource("pageFormats")
    void readsAPercentageTheSameOnEveryPage(PaperSize paperSize, Orientation orientation) {
        ConversionParams conversionParams = ConversionParams.builder().paperSize(paperSize).orientation(orientation).build();

        assertEquals("50%", widthOf("width: 50%;", conversionParams));
        assertEquals("100%", widthOf("width: 100%;", conversionParams), "Exactly the whole width must not read as more than it");
        assertEquals("100%", widthOf("width: 100.1%;", conversionParams));
    }

    @Test
    void leavesATableWhichStatesNoWidth() {
        assertEquals("", widthOf("border: 1px solid black;", A4_PORTRAIT));
    }

    @Test
    void leavesATableWhoseWidthIsNoNumber() {
        // The width comes from the document, and one table written wrong must not fail the whole export
        assertEquals("abcpx", widthOf("width: abcpx;", A4_PORTRAIT));
        // ph-css reads "1.2.3%" as "1.2 .3%", a percentage which is no number
        assertEquals("1.2 .3%", widthOf("width: 1.2.3%;", A4_PORTRAIT));
    }

    private static Stream<Arguments> pageFormats() {
        return Arrays.stream(PaperSize.values())
                .flatMap(paperSize -> Arrays.stream(Orientation.values()).map(orientation -> Arguments.of(paperSize, orientation)));
    }

    private String widthOf(String style, ConversionParams conversionParams) {
        Document document = Jsoup.parse("<table id='test' style='" + style + "'><tr><td>a</td></tr></table>");
        new TableSizeAdjuster(document, conversionParams).execute();

        String adjustedStyle = document.getElementById("test").attr(HtmlTagAttr.STYLE);
        return CssUtils.getPropertyValue(CssUtils.parseDeclarations(adjustedStyle), ECSSProperty.WIDTH);
    }
}
