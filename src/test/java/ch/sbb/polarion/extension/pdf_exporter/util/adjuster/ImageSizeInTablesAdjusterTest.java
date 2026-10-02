package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        CSSDeclarationList cssStyles = parseCss(style);
        String maxWidthStr = CssUtils.getPropertyValue(cssStyles, CssProp.MAX_WIDTH);
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
            mockedTableAnalyzer.when(() -> TableAnalyzer.analyze(any(Element.class), anyInt()))
                    .thenReturn(new TableAnalyzer.TableMetrics(Collections.emptyMap(), 0, List.of()));

            ImageSizeInTablesAdjuster adjuster = new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build());
            adjuster.execute();

            // The image should still be adjusted using the fallback mechanism (columnCountBasedWidth)
            Element testImg = doc.getElementById("test-img");
            assertNotNull(testImg);

            String style = testImg.attr(HtmlTagAttr.STYLE);
            CSSDeclarationList cssStyles = parseCss(style);
            String maxWidthStr = CssUtils.getPropertyValue(cssStyles, CssProp.MAX_WIDTH);
            assertNotNull(maxWidthStr, "max-width should be set even when TableAnalyzer returns empty map");

            float maxWidth = Float.parseFloat(maxWidthStr.replace(Measure.PX, ""));
            // The fallback should use columnCountBasedWidth which is pageWidth / columnsCount = 593 / 2 = 296
            assertTrue(maxWidth > 290 && maxWidth <= 300, "Image should be adjusted using fallback width calculation");
        }
    }

    @Test
    void testImageWhichStatesNoWidthIsNotEnlargedToTheColumn() {
        String html = """
                <table>
                    <tr>
                        <td><img id='test-img' src='tall.svg' style='max-width: 650px;'/></td>
                        <td><img src='placeholder.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        assertEquals("", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH), "A width the image never had would enlarge a narrow image to the column");
        assertNotEquals("", CssUtils.getPropertyValue(cssStyles, CssProp.MAX_WIDTH), "The column is what limits the image");
    }

    @Test
    void testImageWhichStatesItsWidthAsAnAttributeIsFittedToTheColumn() {
        String html = """
                <table>
                    <tr>
                        <td><img id='test-img' src='wide.jpg' width='800' style='max-width: 900px;'/></td>
                        <td><img src='placeholder.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        String width = CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH);
        assertNotEquals("", width, "The width the attribute stated is removed, so the style must carry it");
        assertEquals(CssUtils.getPropertyValue(cssStyles, CssProp.MAX_WIDTH), width, "A width wider than the column is the column's");
    }

    @Test
    void testImageWhichFitsTheColumnKeepsTheWidthItStates() {
        String html = """
                <table>
                    <tr>
                        <td><img id='test-img' src='small.jpg' width='100' style='max-width: 900px;'/></td>
                        <td><img src='placeholder.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        assertEquals("100px", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH), "The column limits an image, it never enlarges one");
    }

    @Test
    void testImageFittedToTheColumnKeepsItsOwnRatio() {
        String html = """
                <table>
                    <tr>
                        <td><img id='test-img' src='wide.jpg' style='width: 800px; height: 300px;'/></td>
                        <td><img src='placeholder.jpg' width='100' height='100' style='width:100px;'/></td>
                    </tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        assertEquals("", CssUtils.getPropertyValue(cssStyles, CssProp.HEIGHT), "A height stated for a width the image no longer has would distort it");
    }

    @Test
    void testImageInATableIsLimitedToThePageLessItsHeader() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr><td><img id='test-img' src='tall.svg'/></td><td>Taller than a page</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        float limit = pixelsOf(cssStyles, CssProp.MAX_HEIGHT);
        assertTrue(limit > 0 && limit < PaperSizeUtils.getMaxHeight(ConversionParams.builder().build()),
                "The page less the height of the header is what the image is given, and it states no size of its own");
    }

    @Test
    void testImageWhichAsksForLessThanThePageKeepsWhatItAsksFor() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr><td><img id='test-img' src='tall.svg' style='max-height: 200px;'/></td><td>Note</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        assertEquals(200f, pixelsOf(cssStyles, CssProp.MAX_HEIGHT), "A limit the document states is smaller than the page, so it stands");
    }

    @Test
    void testImageCutByTheLimitKeepsItsShape() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr><td><img id='test-img' src='tall.jpg' width='300' height='3000'/></td><td>Note</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        CSSDeclarationList cssStyles = parseCss(doc.getElementById("test-img").attr(HtmlTagAttr.STYLE));
        assertEquals(CssProp.OBJECT_FIT_CONTAIN_VALUE, CssUtils.getPropertyValue(cssStyles, CssProp.OBJECT_FIT),
                "The limit cuts into the 3000 px the image states, and a height cut alone squashes the drawing");
    }

    @Test
    void testRowOfAnImageWhichStatesNoSizeIsKeptWhole() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr id='tall-row'><td><img src='tall.svg' style='max-width: 650px;'/></td><td>Taller than a page</td></tr>
                    <tr id='small-row'><td><img src='icon.gif' style='width: 16px;height: 16px;'/></td><td>An icon</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE,
                CssUtils.getPropertyValue(parseCss(doc.getElementById("tall-row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The image states no size, so it can fill the page and the row must carry its header with it");
        assertEquals("", CssUtils.getPropertyValue(parseCss(doc.getElementById("small-row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "An icon leaves the row free to break where a row of text would");
    }

    @Test
    void testRowOfAnImageWhichStatesAHeightThePageCannotHoldIsKeptWhole() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr id='tall-row'><td><img src='tall.svg' style='width: 150px;height: 1500px;'/></td><td>Taller than a page</td></tr>
                    <tr id='short-row'><td><img src='short.svg' style='width: 150px;height: 300px;'/></td><td>A third of a page</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE,
                CssUtils.getPropertyValue(parseCss(doc.getElementById("tall-row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The image states a height the page cannot hold, so the row must carry its header with it");
        assertEquals("", CssUtils.getPropertyValue(parseCss(doc.getElementById("short-row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The page holds the height the image states, and the row breaks where it must");
    }

    @Test
    void testRowOfAnImageWhichStatesAHeightInPointsIsKeptWhole() {
        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE, breakInsideOfARowWith("<img src='tall.svg' style='width: 100pt;height: 1200pt;'/>", "Taller than a page"),
                "A height stated in points is as tall as one stated in pixels");
    }

    @Test
    void testRowOfATallImageTheColumnNarrowsIsKeptWhole() {
        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE, breakInsideOfARowWith("<img src='tall.svg' style='width: 3000px;height: 30000px;'/>", "Wider than a column"),
                "Narrowed to its column in the shape it states, the image is still taller than the page leaves it");
    }

    @Test
    void testRowOfAThumbnailTheColumnNarrowsBreaks() {
        assertEquals("", breakInsideOfARowWith("<img src='wide.svg' style='width: 3000px;height: 1500px;'/>", "Wider than a column"),
                "Narrowed to its column in the shape it states, the image is low, and the row breaks where it must");
    }

    @Test
    void testRowOfAnImageWhichStatesAHeightThePageHoldsBreaks() {
        assertEquals("", breakInsideOfARowWith("<img src='short.svg' style='width: 150px;height: 300px;'/>", "A third of a page"),
                "The page holds the image, and the row breaks where it must");
    }

    @Test
    void testRowWhoseTextRunsOverAPageBreaks() {
        assertEquals("", breakInsideOfARowWith("<img src='tall.svg' style='width: 150px;height: 1500px;'/>", "A line of text. ".repeat(800)),
                "The text of the row runs over a page and splits the row anyway, so keeping it whole would only leave a page blank");
    }

    /** The export moves a row of header cells into a thead, and the rest of the row is measured all the same. */
    @Test
    void testRowWhoseTextRunsOverAPageBreaksUnderAHead() {
        String html = """
                <table>
                    <thead><tr><th>Diagram</th><th>Note</th></tr></thead>
                    <tbody><tr id='row'><td><img src='tall.svg' style='width: 150px;height: 1500px;'/></td><td>%s</td></tr></tbody>
                </table>
                """.formatted("A line of text. ".repeat(800));

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals("", CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The text of the row runs over a page and splits the row anyway");
    }

    /** The outer table fits an image of a nested one first, and the row of the nested table is still kept whole. */
    @Test
    void testRowOfANestedTableWhoseImageTheOuterTableShortensIsKeptWhole() {
        String html = """
                <table>
                    <tr><td style='width: 200px;'>
                        <table>
                            <tr><th>Diagram</th></tr>
                            <tr id='row'><td><img src='tall.svg' style='width: 900px;height: 9000px;'/></td></tr>
                        </table>
                    </td><td>A cell beside it</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE,
                CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The image is drawn as tall as the page leaves it, so its row carries the header of its own table");
    }

    /** A header which holds a tall image leaves the rows under it the page less the header as it is drawn, its image fitted. */
    @Test
    void testRowUnderAHeaderWithATallImageIsMeasuredUnderTheHeaderAsDrawn() {
        String html = """
                <table>
                    <tr><th><img src='logo.svg' style='width: 150px;height: 1500px;'/></th><th>Note</th></tr>
                    <tr id='row'><td><img src='tall.svg' style='width: 150px;height: 1500px;'/></td><td>%s</td></tr>
                </table>
                """.formatted("A line of text. ".repeat(60));

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE, CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The row fits under the header as it is drawn, its image fitted to the page, so it carries the header with it");
    }

    /** A caption under the image in its cell takes room of the page, and the image gives that height up. */
    @Test
    void testImageWithACaptionIsLimitedToThePageLessTheCaption() {
        String alone = "<img id='diagram' src='tall.svg' style='max-width: 650px;'/>";
        String withCaption = alone + "<p>Figure 1: A chain of steps, as tall as a page. Each step leads to the next one, from the first to the last.</p>";

        float limitAlone = maxHeightOfTheImageIn(alone);
        float limitWithCaption = maxHeightOfTheImageIn(withCaption);

        assertTrue(limitWithCaption < limitAlone - 20, "The caption takes " + (limitAlone - limitWithCaption) + " px of the page from the image");
    }

    /** A caption takes the text styles of the elements around its table, as it prints. */
    @Test
    void testCaptionIsMeasuredInTheTextStylesAroundItsTable() {
        String alone = "<img id='diagram' src='tall.svg' style='max-width: 650px;'/>";
        String withCaption = alone + "<p>Figure 1: A chain of steps, as tall as a page. Each step leads to the next one.</p>";
        String largeText = "font-size: 24pt; line-height: 2;";

        float captionInPlainText = maxHeightOfTheImageIn(alone) - maxHeightOfTheImageIn(withCaption);
        float captionInLargeText = maxHeightOfTheImageIn(alone, largeText) - maxHeightOfTheImageIn(withCaption, largeText);

        assertTrue(captionInLargeText > captionInPlainText + 20,
                "The caption takes " + captionInPlainText + " px in plain text and " + captionInLargeText + " px in large text");
    }

    /** Lines which hold no text, and another image of the cell, take room of the page too. */
    @Test
    void testLinesAndOtherImagesOfTheCellTakeTheirRoom() {
        float alone = maxHeightOfTheImageIn("<img id='diagram' src='tall.svg' style='max-width: 650px;'/>");
        float withLines = maxHeightOfTheImageIn("<img id='diagram' src='tall.svg' style='max-width: 650px;'/><br/><br/><br/><br/>");
        float withAnImage = maxHeightOfTheImageIn("<img id='diagram' src='tall.svg' style='max-width: 650px;'/><img src='other.svg' style='width: 100px;height: 200px;'/>");

        assertTrue(withLines < alone - 20, "The empty lines take " + (alone - withLines) + " px of the page");
        assertTrue(withAnImage < alone - 150, "The other image, on a line under the diagram, takes " + (alone - withAnImage) + " px of the page");
    }

    /** A row whose text runs over a page splits anyway, and keeps the break it states, whatever image it holds. */
    @Test
    void testRowWhoseTextRunsOverAPageKeepsTheBreakItStates() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr id='row' style='break-inside: auto;'><td style='width: 200px;'><img src='tall.svg' style='max-width: 650px;'/></td><td>%s</td></tr>
                </table>
                """.formatted("A line of text. ".repeat(800));

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals("auto", CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "Kept whole, the row would only move its text to the next page and leave this one blank");
    }

    /** Two images side by side share the height of their line, and neither takes the height of the other from the page. */
    @Test
    void testImagesSideBySideShareTheirHeight() {
        float alone = maxHeightOfTheImageIn("<img id='diagram' src='tall.svg' style='width: 80px;height: 1500px;'/>");
        float besideAnother = maxHeightOfTheImageIn("<img id='diagram' src='tall.svg' style='width: 80px;height: 1500px;'/><img src='other.svg' style='width: 80px;height: 500px;'/>");

        assertEquals(alone, besideAnother, 10f, "The image beside it shares the line of the diagram");
    }

    /** An image which states only a width the column narrows is drawn in the shape of its file, and its row is kept whole. */
    @Test
    void testRowOfAnImageWhichStatesOnlyAWidthTheColumnNarrowsIsKeptWhole() {
        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE, breakInsideOfARowWith("<img src='tall.svg' width='800' style='max-width: 900px;'/>", "Wider than a column"),
                "The height of the image follows its file, which is not known before it is embedded");
    }

    private float maxHeightOfTheImageIn(String cell) {
        return maxHeightOfTheImageIn(cell, "");
    }

    private float maxHeightOfTheImageIn(String cell, String textStyle) {
        Document doc = Jsoup.parse("""
                <div style='%s'><table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr><td style='width: 200px;'>%s</td><td>Taller than a page</td></tr>
                </table></div>
                """.formatted(textStyle, cell));
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();
        return pixelsOf(parseCss(doc.getElementById("diagram").attr(HtmlTagAttr.STYLE)), CssProp.MAX_HEIGHT);
    }

    @Test
    void testRowWhichLetsItselfBreakIsKeptWholeAllTheSame() {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr id='row' style='break-inside: auto;'><td><img src='tall.svg' style='width: 150px;height: 1500px;'/></td><td>Taller than a page</td></tr>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();

        assertEquals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE, CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE),
                "The image does not split, so a break of the row would only leave the header alone: the table moves on whole");
    }

    private String breakInsideOfARowWith(String image, String note) {
        String html = """
                <table>
                    <tr><th>Diagram</th><th>Note</th></tr>
                    <tr id='row'><td>%s</td><td>%s</td></tr>
                </table>
                """.formatted(image, note);

        Document doc = Jsoup.parse(html);
        new ImageSizeInTablesAdjuster(doc, ConversionParams.builder().build()).execute();
        return CssUtils.getPropertyValue(parseCss(doc.getElementById("row").attr(HtmlTagAttr.STYLE)), CssProp.BREAK_INSIDE);
    }

    private float pixelsOf(CSSDeclarationList cssStyles, String property) {
        String value = CssUtils.getPropertyValue(cssStyles, property);
        assertTrue(value.endsWith(Measure.PX), property + " is stated in pixels, and reads '" + value + "'");
        try {
            return Float.parseFloat(value.replace(Measure.PX, ""));
        } catch (NumberFormatException e) {
            return fail(property + " reads '" + value + "', which is no number");
        }
    }

    private CSSDeclarationList parseCss(String style) {
        return Optional.ofNullable(CSSReaderDeclarationList.readFromString(style)).orElse(new CSSDeclarationList());
    }
}
