package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.CSSOMParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.io.StringReader;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ImageSizeInTablesAdjusterTest {

    @Test
    void getImageWidthBasedOnColumnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(-1, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><img id='test'></tr></table>").getElementById("test")));
        assertEquals(296, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td><td><img id='test'></td></table>").getElementById("test")));
        assertEquals(197, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td><td><img id='test'></td><td></td></tr></table>").getElementById("test")));

        assertEquals(437, new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().orientation(Orientation.LANDSCAPE).paperSize(PaperSize.A3).build()).getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td></tr><tr><td></td><td><img id='test'></td><td></td></tr><tr><td></td></tr></table>").getElementById("test")));
    }


    @Test
    void columnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(0, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'></table>").getElementById("test")));
        assertEquals(3, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td></td><td><span/></td><td></table>").getElementById("test")));
        assertEquals(4, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td colspan='2'></td><td></td><td></table>").getElementById("test")));
        assertEquals(8, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td colspan='5'></td><td colspan='2'></td><td></table>").getElementById("test")));
    }

    @Test
    void testImageInColspanCell() {
        String html = """
                <table>
                    <tr>
                        <td><img src='placeholder1.jpg' width='100' height='100' style='width:100px;'/></td>
                        <td><img src='placeholder2.jpg' width='100' height='100' style='width:100px;'/></td>
                        <td><img src='placeholder3.jpg' width='100' height='100' style='width:100px;'/></td>
                        <td><img src='placeholder4.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                    <tr>
                        <td colspan='2'><img id='test-img' src='large.jpg' width='500' height='300' style='width:500px;'/></td>
                        <td><img src='placeholder5.jpg' width='100' height='100' style='width:100px;'/></td>
                        <td><img src='placeholder6.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        ImageSizeInTablesAdjuster adjuster = new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build());
        adjuster.execute();

        // The image in the cell with colspan=2 should have its width adjusted to fit within the combined width of 2 columns
        Element testImg = doc.getElementById("test-img");
        assertNotNull(testImg);

        String style = testImg.attr(HtmlTagAttr.STYLE);
        CSSStyleDeclaration cssStyle = parseCss(style);
        String maxWidthStr = cssStyle.getPropertyValue(CssProp.MAX_WIDTH);
        assertNotNull(maxWidthStr);
        float maxWidth = Float.parseFloat(maxWidthStr.replace(Measure.PX, ""));
        assertTrue(maxWidth > 400 && maxWidth < 450);
    }

    @Test
    void testImageAdjustmentWhenTableAnalyzerReturnsEmptyMap() {
        String html = """
                <table>
                    <tr>
                        <td><img id='test-img' src='large.jpg' width='800' height='600' style='width:800px;'/></td>
                        <td><img src='placeholder.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);

        // Mock TableAnalyzer.getColumnWidths to return an empty map
        try (var mockedTableAnalyzer = mockStatic(TableAnalyzer.class)) {
            mockedTableAnalyzer.when(() -> TableAnalyzer.getColumnWidths(any(Element.class), anyInt()))
                    .thenReturn(Collections.emptyMap());

            ImageSizeInTablesAdjuster adjuster = new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build());
            adjuster.execute();

            // The image should still be adjusted using the fallback mechanism (columnCountBasedWidth)
            Element testImg = doc.getElementById("test-img");
            assertNotNull(testImg);

            String style = testImg.attr(HtmlTagAttr.STYLE);
            CSSStyleDeclaration cssStyle = parseCss(style);
            String maxWidthStr = cssStyle.getPropertyValue(CssProp.MAX_WIDTH);
            assertNotNull(maxWidthStr, "max-width should be set even when TableAnalyzer returns empty map");

            float maxWidth = Float.parseFloat(maxWidthStr.replace(Measure.PX, ""));
            // The fallback should use columnCountBasedWidth which is pageWidth / columnsCount = 593 / 2 = 296
            assertTrue(maxWidth > 290 && maxWidth <= 300, "Image should be adjusted using fallback width calculation");
        }
    }

    protected CSSStyleDeclaration parseCss(String style) {
        try {
            return new CSSOMParser().parseStyleDeclaration(new InputSource(new StringReader(style)));
        } catch (Exception e) {
            return new CSSStyleDeclarationImpl();
        }
    }
}
