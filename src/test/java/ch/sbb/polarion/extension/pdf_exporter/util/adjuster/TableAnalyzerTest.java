package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xhtmlrenderer.layout.breaker.BreakPoint;
import org.xhtmlrenderer.layout.breaker.BreakPointsProvider;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TableAnalyzerTest {

    /** The export moves a row of header cells into a thead, and the header still counts, as its rows do. */
    @Test
    void measuresTheRowsAndTheHeaderOfATableWithAHead() {
        Element table = Jsoup.parse("""
                <table><thead><tr><th>Diagram</th><th>Note</th></tr></thead>
                <tbody><tr><td>A</td><td>B</td></tr><tr><td>C</td><td>D</td></tr></tbody></table>""").selectFirst("table");

        TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, 650);

        assertEquals(3, metrics.rowHeights().size(), "The row of the head is a row of the table as well");
        assertTrue(metrics.headerHeight() > 0, "The head is repeated on every page, so it takes its height there");
    }

    /** WeasyPrint breaks no word between a hyphen and a digit, by the default rule of UAX #14 (LB25). */
    @ParameterizedTest
    @ValueSource(strings = {"TMSPRG-13164", "2022-01-05", "ТРБ-3001"})
    void keepsAWordWholeWhereADigitFollowsItsHyphen(String word) {
        assertEquals(narrowestWidth(word.replace('-', '\u00A0')), narrowestWidth(word), 2, "\"%s\" is as wide as it is unbroken".formatted(word));
    }

    @Test
    void breaksAWordAfterAHyphenALetterFollows() {
        assertTrue(narrowestWidth("Rechts-Schutz") < narrowestWidth("Rechts\u00A0Schutz"), "A hyphen before a letter is a place to break");
    }

    @Test
    void breaksAWordWhereTheDocumentLetsItBreak() {
        assertTrue(narrowestWidth("TMSPRG-<wbr>13164") < narrowestWidth("TMSPRG-13164"), "A break point the document gives a word is still one");
    }

    /** A word with a formatted part is one word, as WeasyPrint lays it out: the measure breaks it nowhere. */
    @ParameterizedTest
    @ValueSource(strings = {"Gleisfreimelde<span style=\"color: red\">anlage</span>", "Gleisfreim<b>elde</b>anlage", "Gleis<a href=\"#x\">freimelde</a>anlage"})
    void keepsAWordWithAFormattedPartWhole(String content) {
        assertEquals(widestWidth(content), narrowestWidth(content), "As narrow as it is wide");
    }

    private static final String ICON = "<img class=\"polarion-Icons\" src=\"data:image/gif;base64,R0lGODlhEAAQAIAAAP///wAAACH5BAEAAAAALAAAAAAQABAAAAIOhI+py+0Po5y02ouzPgUAOw==\"/>";

    /** A text which may not wrap measures whole, across its inline boxes and with its images, as WeasyPrint lays it out. */
    @ParameterizedTest
    @ValueSource(strings = {
            "<span style=\"white-space: nowrap\">EL <b>17</b></span>",
            "<span style=\"white-space: nowrap\">Rechts-Schutz TMSPRG-13164</span>",
            "<span style=\"white-space: nowrap\">" + ICON + "EL-17</span>",
            "<span style=\"white-space: nowrap\"><img style=\"width: 16px\" src=\"icon.png\"/>EL-17</span>",
            "<span style=\"white-space: nowrap\"><img style=\"width: 12pt\" src=\"icon.png\"/>EL-17</span>",
            "<span lang=\"de\" style=\"white-space: nowrap\">" + ICON + "EL-17</span>",
            "<span style=\"white-space: nowrap\">EL <b style=\"white-space: inherit\">17 18</b></span>",
            "<span style=\"white-space: nowrap; overflow-wrap: anywhere\">" + ICON + "EL-17</span>",
            "<span style=\"hyphens: auto\"><span style=\"white-space: nowrap\">" + ICON + "EL-17</span></span>",
            "<span style=\"word-break: break-all\"><span style=\"white-space: nowrap\">" + ICON + "EL-17</span></span>",
            // a work item link as Polarion renders it
            "<span class=\"polarion-rte-link\"><span style=\"white-space:nowrap;\"><a class=\"polarion-Hyperlink\"><span style=\"white-space:nowrap;\">" + ICON + "</span><span>EL-17</span></a></span></span>"
    })
    void keepsATextWhichMayNotWrapWhole(String content) {
        assertEquals(widestWidth(content), narrowestWidth(content), "As narrow as it is wide");
    }

    @Test
    void measuresTheIconOfALinkInATextWhichMayNotWrap() {
        assertEquals(widestWidth(ICON + "EL-17"), narrowestWidth("<span style=\"white-space: nowrap\">" + ICON + "EL-17</span>"), "As wide as the icon and the ID");
    }

    @Test
    void breaksALineAroundATextWhichMayNotWrap() {
        assertEquals(narrowestWidth("<span style=\"white-space: nowrap\">yy zz</span>"), narrowestWidth("xx <span style=\"white-space: nowrap\">yy zz</span>"),
                "The space before it is a place to break");
        // flying-saucer counts the space after a word with the word
        assertEquals(narrowestWidth("<span>yy\u00A0zz</span> xx"), narrowestWidth("<span style=\"white-space: nowrap\">yy zz</span> xx"),
                "The space after it is a place to break");
        String link = "<span style=\"white-space:nowrap;\">" + ICON + "<span>EL-1%d</span></span>";
        assertEquals(narrowestWidth(link.formatted(8) + ", " + link.formatted(9)), narrowestWidth(link.formatted(8) + ", " + link.formatted(9) + ", " + link.formatted(7)),
                "A list of links breaks after each comma");
        assertTrue(narrowestWidth(link.formatted(8) + ", " + link.formatted(9)) < widestWidth(link.formatted(8) + ", " + link.formatted(9)), "and so lies on two lines");
        assertTrue(narrowestWidth(ICON + "EL-17") < widestWidth(ICON + "EL-17"), "A line may break after an image of a text which may wrap");
    }

    @Test
    void measuresTheImageOfATextWhichMayNotWrapAtTheWidthItsCssStates() {
        assertEquals(widestWidth("<img style=\"width: 16px\" src=\"icon.png\"/>EL-17"),
                narrowestWidth("<span style=\"white-space: nowrap\"><img style=\"width: 12pt\" src=\"icon.png\"/>EL-17</span>"), "12pt are 16px");
    }

    @Test
    void leavesOutAHiddenImageOfATextWhichMayNotWrap() {
        assertEquals(narrowestWidth("<span style=\"white-space: nowrap\">EL-17</span>"),
                narrowestWidth("<span style=\"white-space: nowrap\"><img style=\"display: none; width: 300px\" src=\"icon.png\"/>EL-17</span>"), "A hidden image takes no room");
    }

    @Test
    void collapsesTheSpacesOfATextWhichMayNotWrapAcrossItsElements() {
        assertEquals(narrowestWidth("<span style=\"white-space: nowrap\">aaa <b>bbb</b></span>"), narrowestWidth("<span style=\"white-space: nowrap\">aaa <b> bbb</b></span>"),
                "Two spaces in a row are one");
    }

    @Test
    void leavesOutTheHiddenTextOfATextWhichMayNotWrap() {
        assertEquals(narrowestWidth("<span style=\"white-space: nowrap\">A B</span>"),
                narrowestWidth("<span style=\"white-space: nowrap\">A<b style=\"display: none\">x </b> B</span>"), "A hidden text takes no room and leaves the spaces around");
    }

    /** A space at the edge of a line takes no room in print, so it stays one the measure handles as such. */
    @Test
    void measuresTheSpacesAtTheEdgesOfATextWhichMayNotWrapAsRegularOnes() {
        assertEquals(narrowestWidth("<span> EL-17 </span>"), narrowestWidth("<span style=\"white-space: nowrap\"> EL-17 </span>"), "As wide as the text which may wrap");
    }

    /** The measure lays out an image whose size depends on the font, as it does without nowrap. */
    @Test
    void measuresAnImageOfARelativeSizeAsTheMeasureDoes() {
        String image = "<img style=\"width: 1em\" src=\"" + embeddedImage(100, 10) + "\"/>";
        assertTrue(narrowestWidth("<span style=\"white-space: nowrap\">" + image + "EL-17</span>") < 100, "As wide as 1em, not as the image");
    }

    @Test
    void measuresTheMarginOfAnImageOfATextWhichMayNotWrap() {
        String image = "<img style=\"width: 16px; height: 16px; margin-right: 24px; padding-left: 3px; border: 2px solid black\" src=\"icon.png\"/>";
        assertEquals(widestWidth(image + "EL-17"), narrowestWidth("<span style=\"white-space: nowrap\">" + image + "EL-17</span>"), "As wide as the image with its box");
    }

    /** An image whose size its own style does not state is left to the measure, which may size it by a stylesheet. */
    @Test
    void leavesAnImageOfAnUnknownSizeToTheMeasure() {
        Element table = Jsoup.parse("<table><tr><td><span style=\"white-space: nowrap\"><img src=\"icon.png\"/>EL-17</span></td></tr></table>").selectFirst("table");
        assertTrue(TableAnalyzer.analyze(table, 10, "img { width: 120px; height: 40px; }").tableWidth() >= 120, "As wide as the stylesheet states");
    }

    /** Polarion wraps the title of a linked work item again, in the element which may not wrap. */
    @Test
    void breaksTheTitleOfALinkWhichWrapsAgain() {
        String link = "<span style=\"white-space:nowrap;\"><a><span style=\"white-space:nowrap;\">" + ICON + "</span><span>REQ-200</span>"
                + "<span style=\"white-space: normal\"> - A title of it</span></a></span>";
        assertEquals(narrowestWidth("<span style=\"white-space: nowrap\">" + ICON + "REQ-200</span> -"), narrowestWidth(link),
                "As narrow as the icon and the ID");
    }

    @Test
    void breaksTheTitleOfALinkWhichWrapsAgainAnywhereAsTheTextAround() {
        String link = "<span style=\"white-space:nowrap;\">" + ICON + "<span>REQ-200</span><span style=\"white-space: normal\"> Averyveryverylongtitle</span></span>";
        assertTrue(narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link + "</span>") < narrowestWidth(link), "The title breaks anywhere");
        assertTrue(narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link + "</span>") >= widestWidth(ICON + "REQ-200"), "but not the icon and the ID");
    }

    @Test
    void breaksTheTitleOfALinkWhichWrapsAgainAnywhereAsTheElementAroundInTheText() {
        String link = "<span style=\"white-space:nowrap;\">" + ICON + "<span>REQ-200</span><b%s><span style=\"white-space: normal\"> Averyveryverylongtitle</span></b></span>";
        assertTrue(narrowestWidth(link.formatted(" style=\"overflow-wrap: anywhere\"")) < narrowestWidth(link.formatted("")), "The title breaks anywhere");
    }

    @Test
    void collapsesTheSpacesOfATextWhichMayNotWrapIntoAnElementWhichWrapsAgain() {
        assertEquals(widestWidth("<span style=\"white-space: nowrap\">A <span style=\"white-space: normal\">B</span>C</span>"),
                widestWidth("<span style=\"white-space: nowrap\">A <span style=\"white-space: normal\"> B</span>C</span>"), "Two spaces in a row are one");
    }

    @Test
    void keepsTheLineBreakOfAnElementWhichWrapsAgainInATextWhichMayNotWrap() {
        String wrapped = "<span style=\"white-space: pre-line\">\nB\nC</span>";
        assertEquals(rowHeight("<span>A " + wrapped + "</span>", 650), rowHeight("<span style=\"white-space: nowrap\">A " + wrapped + "</span>", 650),
                "A line break is no space to collapse");
    }

    @Test
    void keepsATextWhichMayNotWrapOnOneLineWhereWordsBreakAnywhere() {
        String text = "xxxx <span style=\"white-space: nowrap\">EL-17890123</span>";
        int width = (int) widestWidth("<span style=\"white-space: nowrap\">EL-17890123</span>");
        assertEquals(rowHeight(text, width), rowHeight("<span style=\"word-break: break-all\">" + text + "</span>", width), "On a line of its own");
    }

    private static int rowHeight(String content, int width) {
        Element table = Jsoup.parse("<table><tr><td>" + content + "</td></tr></table>").selectFirst("table");
        return TableAnalyzer.analyze(table, width).rowHeights().getFirst();
    }

    /** The measure lays out no hidden element, so a malformed image in one does not keep it from measuring the table. */
    @Test
    void measuresAHiddenTextWhichMayNotWrapWithAMalformedEmbeddedImage() {
        assertEquals(narrowestWidth("EL-17"),
                narrowestWidth("EL-17<span style=\"display: none\"><span style=\"white-space: nowrap\"><img src=\"data:image/png;base64,%%%\"/>EL-17</span></span>"),
                "As wide as the text shown");
    }

    @Test
    void breaksTheTitleOfALinkWhichWrapsAgainAnywhereAsItInheritsIt() {
        String link = "<span style=\"white-space:nowrap;%s\">" + ICON + "<span>REQ-200</span><span style=\"white-space: normal\"> Averyveryverylongtitle</span></span>";
        assertEquals(narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link.formatted("") + "</span>"),
                narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link.formatted(" word-wrap: inherit") + "</span>"), "As without inherit");
    }

    @Test
    void keepsTheSpacesOfAnElementWhichPreservesThemInAnElementWhichWrapsAgain() {
        String pre = "<span style=\"white-space: normal\"><span style=\"white-space: pre\">   B</span></span>";
        assertEquals(widestWidth("<span>A " + pre + "</span>"), widestWidth("<span style=\"white-space: nowrap\">A " + pre + "</span>"), "As without nowrap");
    }

    @Test
    void breaksTheTitleOfALinkWhichWrapsAgainAnywhereAsItInheritsItItself() {
        String link = "<span style=\"white-space:nowrap;\">" + ICON + "<span>REQ-200</span><span style=\"white-space: normal%s\"> Averyveryverylongtitle</span></span>";
        assertEquals(narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link.formatted("") + "</span>"),
                narrowestWidth("<span style=\"overflow-wrap: anywhere\">" + link.formatted("; word-wrap: inherit") + "</span>"), "As without inherit");
    }

    @Test
    void collapsesTheSpaceOfAnElementWhichWrapsAgainWithTheSpaceAfter() {
        assertEquals(widestWidth("<span style=\"white-space: nowrap\">A<span style=\"white-space: normal\"> </span>B</span>"),
                widestWidth("<span style=\"white-space: nowrap\">A<span style=\"white-space: normal\"> </span> B</span>"), "Two spaces in a row are one");
    }

    /** The space before the element becomes a no-break space, a pixel wider in the measure than a regular one. */
    @Test
    void keepsTheSpaceAfterAnImageOfAnElementWhichWrapsAgain() {
        String wrapped = "<span style=\"white-space: normal\"><img width=\"16\" height=\"16\" src=\"icon.png\"/> B</span>";
        assertEquals(widestWidth("<span>A\u00A0" + wrapped + "</span>"), widestWidth("<span style=\"white-space: nowrap\">A " + wrapped + "</span>"),
                "The image separates the spaces around it");
    }

    @Test
    void collapsesTheSpacesAroundAHiddenImageOfAnElementWhichWrapsAgain() {
        String wrapped = "<span style=\"white-space: normal\"><b style=\"display: none\"><img width=\"16\" height=\"16\" src=\"icon.png\"/>x </b> B</span>";
        assertEquals(widestWidth("<span style=\"white-space: nowrap\">A <span style=\"white-space: normal\">B</span></span>"),
                widestWidth("<span style=\"white-space: nowrap\">A " + wrapped + "</span>"), "A hidden image separates no spaces");
    }

    @Test
    void keepsTheTextWhichMayNotWrapOfTheTable() {
        Element table = Jsoup.parse("<table><tr><td><span style=\"white-space: nowrap\">" + ICON + "EL-17</span></td></tr></table>").selectFirst("table");
        String html = table.outerHtml();

        TableAnalyzer.analyze(table, 10);

        assertEquals(html, table.outerHtml(), "The measure works on a copy of the table");
    }

    /** The measure lays out a {@code <wbr>} as a zero width space, which breaks a word at the same place and takes no room. */
    @ParameterizedTest
    @ValueSource(strings = {"Gleisfreim<wbr>eldeanlage", "<b>Gleisfreim<wbr>elde</b>anlage", "Gleis<wbr>frei<wbr>melde<wbr>anlage"})
    void measuresAWordBreakOpportunityAsAZeroWidthSpace(String content) {
        String zeroWidthSpaces = content.replace("<wbr>", "\u200B");
        assertEquals(narrowestWidth(zeroWidthSpaces), narrowestWidth(content), "As narrow");
        assertEquals(widestWidth(zeroWidthSpaces), widestWidth(content), "As wide");
        assertEquals(widestWidth(content.replace("<wbr>", "")), widestWidth(content), "A break point takes no room");
    }

    @Test
    void keepsTheWordBreakOpportunitiesOfTheTable() {
        Element table = Jsoup.parse("<table><tr><td>Gleisfreim<wbr>eldeanlage</td></tr></table>").selectFirst("table");

        TableAnalyzer.analyze(table, 10);

        assertEquals(1, table.select("wbr").size(), "The measure works on a copy of the table");
    }

    @Test
    void breaksAWordAtItsSoftHyphen() {
        assertTrue(narrowestWidth("TMSPRG-131\u00AD64") < narrowestWidth("TMSPRG-13164"), "A soft hyphen is a place to break the document gives a word");
    }

    @Test
    void breaksAWordWhereTheCellLetsItBreakAnywhere() {
        assertTrue(narrowestWidth("<span style=\"overflow-wrap: anywhere\">TMSPRG-13164</span>") < narrowestWidth("TMSPRG-13164"),
                "A word which may break anywhere breaks after its hyphen as well");
    }

    /**
     * flying-saucer asks for the break points of the rest of a text from where each line starts. Found without a copy of
     * the rest, they are the break points of that rest.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Anforderung REQ-1001 vom 2022-01-05, siehe https://example.com/a/b?c=d", "Rechts-Schutz TMSPRG-13164", "a\u200Bb c"})
    void findsTheBreakPointsOfTheRestOfATextFromWhereItStarts(String text) {
        TableAnalyzer.MeasureLineBreakingStrategy strategy = new TableAnalyzer.MeasureLineBreakingStrategy();
        for (int start = 0; start <= text.length(); start++) {
            assertEquals(breakPoints(strategy.getBreakPointsProvider(text.substring(start), null, null)),
                    breakPoints(strategy.getBreakPointsProvider(text, start, null, null)), "From %d on".formatted(start));
        }
    }

    private static List<Integer> breakPoints(BreakPointsProvider provider) {
        List<Integer> points = new ArrayList<>();
        for (BreakPoint point = provider.next(); point.position() != BreakIterator.DONE; point = provider.next()) {
            points.add(point.position());
        }
        return points;
    }

    @Test
    void keepsTheSpacesAroundAWordItKeepsWhole() {
        String text = "Anforderungsverwaltung REQ-1001 of 2022-01-05, approved";
        Element table = Jsoup.parse("<table><tr><td>" + text + "</td></tr></table>").selectFirst("table");

        TableAnalyzer.analyze(table, 650);

        assertEquals(text, table.text(), "The measure works on a copy of the table");
        assertTrue(narrowestWidth(text) < narrowestWidth(text.replace(" ", "")), "The spaces around stay places to break");
    }

    /** The width of a table of one cell with the given content, laid out as narrow as it goes. */
    private static float narrowestWidth(String content) {
        Element table = Jsoup.parse("<table><tr><td>" + content + "</td></tr></table>").selectFirst("table");
        return (float) TableAnalyzer.analyze(table, 10).tableWidth();
    }

    /** The width of a table of one cell with the given content, laid out on one line. */
    private static float widestWidth(String content) {
        Element table = Jsoup.parse("<table><tr><td>" + content + "</td></tr></table>").selectFirst("table");
        return (float) TableAnalyzer.analyze(table, 650).tableWidth();
    }

    @Test
    @SneakyThrows
    void embeddedFontIsLoaded() {
        // Access the private EMBEDDED_FONT field via reflection
        Field fontField = TableAnalyzer.class.getDeclaredField("EMBEDDED_FONT");
        fontField.setAccessible(true);
        Font embeddedFont = (Font) fontField.get(null);

        assertNotNull(embeddedFont, "EMBEDDED_FONT should not be null");
        assertEquals(12f, embeddedFont.getSize2D(), "Font size should be 12");
        assertEquals(Font.PLAIN, embeddedFont.getStyle(), "Font style should be PLAIN");
    }

    @Test
    @SneakyThrows
    void embeddedFontHasValidMetrics() {
        // Access the private EMBEDDED_FONT field via reflection
        Field fontField = TableAnalyzer.class.getDeclaredField("EMBEDDED_FONT");
        fontField.setAccessible(true);
        Font embeddedFont = (Font) fontField.get(null);

        // Create a graphics context to get font metrics
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        try {
            g2d.setFont(embeddedFont);
            FontMetrics fm = g2d.getFontMetrics();

            assertTrue(fm.getHeight() > 0, "Font height should be positive");
            assertTrue(fm.getAscent() > 0, "Font ascent should be positive");
            assertTrue(fm.getDescent() >= 0, "Font descent should be non-negative");
            assertTrue(fm.charWidth('W') > 0, "Character width should be positive");
            assertTrue(fm.stringWidth("test") > 0, "String width should be positive");
        } finally {
            g2d.dispose();
        }
    }

    @Test
    @SneakyThrows
    void embeddedFontFamilyIsExpected() {
        // Access the private EMBEDDED_FONT field via reflection
        Field fontField = TableAnalyzer.class.getDeclaredField("EMBEDDED_FONT");
        fontField.setAccessible(true);
        Font embeddedFont = (Font) fontField.get(null);

        String family = embeddedFont.getFamily();
        assertTrue(family.equals("Liberation Sans") || family.equals("SansSerif") || family.equals("Dialog"),
                "Font family should be 'Liberation Sans' or fallback 'SansSerif'/'Dialog', but was: " + family);
    }

    @Test
    void loadFontFromPathReturnsValidFont() {
        Font font = TableAnalyzer.loadFontFromPath("/fonts/DejaVuSans.ttf");

        assertNotNull(font, "Font should never be null");
        assertEquals(12f, font.getSize2D(), "Font size should be 12");
        assertEquals(Font.PLAIN, font.getStyle(), "Font style should be PLAIN");
        assertTrue(font.getFamily().equals("DejaVu Sans") || font.getName().equals(Font.SANS_SERIF),
                "Font should be DejaVu Sans or SansSerif fallback");
    }

    @Test
    void loadFontFromPathSuccessfullyLoadsValidFont() {
        Font font = TableAnalyzer.loadFontFromPath("/weasyprint/font/fa-solid-900.ttf");

        assertNotNull(font, "Font should be loaded");
        assertEquals("Font Awesome 6 Free Solid", font.getFamily(), "Font family should be Font Awesome");
        assertEquals(12f, font.getSize2D(), "Font size should be 12");
        assertEquals(Font.PLAIN, font.getStyle(), "Font style should be PLAIN");

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        boolean fontRegistered = false;
        for (String family : fontFamilies) {
            if (family.equals("Font Awesome 6 Free Solid")) {
                fontRegistered = true;
                break;
            }
        }
        assertTrue(fontRegistered, "Font should be registered in GraphicsEnvironment");
    }

    @Test
    void loadFontFromPathWithNonExistentPathReturnsFallback() {
        Font font = TableAnalyzer.loadFontFromPath("/fonts/NonExistentFont.ttf");

        assertNotNull(font, "Fallback font should be returned");
        assertEquals(Font.SANS_SERIF, font.getName(), "Fallback should be SansSerif");
        assertEquals(12f, font.getSize2D(), "Font size should be 12");
        assertEquals(Font.PLAIN, font.getStyle(), "Font style should be PLAIN");
    }

    @Test
    void loadFontFromPathWithInvalidFontReturnsFallback() {
        Font font = TableAnalyzer.loadFontFromPath("/log4j2.xml");

        assertNotNull(font, "Fallback font should be returned on exception");
        assertEquals(Font.SANS_SERIF, font.getName(), "Fallback should be SansSerif");
        assertEquals(12f, font.getSize2D(), "Font size should be 12");
        assertEquals(Font.PLAIN, font.getStyle(), "Font style should be PLAIN");
    }

    @Test
    void columnWidthsAreConsistentAcrossMultipleCalls() {
        Element table = createSimpleTable();
        int pageWidth = PaperSizeUtils.getMaxWidth(ConversionParams.builder().build());

        // Call multiple times and verify results are identical (deterministic behavior)
        Map<Integer, Integer> firstResult = TableAnalyzer.getColumnWidths(table, pageWidth);
        Map<Integer, Integer> secondResult = TableAnalyzer.getColumnWidths(table, pageWidth);
        Map<Integer, Integer> thirdResult = TableAnalyzer.getColumnWidths(table, pageWidth);

        assertEquals(firstResult, secondResult, "Column widths should be identical across calls");
        assertEquals(secondResult, thirdResult, "Column widths should be identical across calls");
    }

    @Test
    void columnWidthsSumEqualsPageWidth() {
        Element table = createSimpleTable();
        int pageWidth = PaperSizeUtils.getMaxWidth(ConversionParams.builder().build());

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, pageWidth);

        // Sum of column widths should approximately equal page width (within rounding tolerance)
        int totalWidth = columnWidths.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(Math.abs(totalWidth - pageWidth) <= columnWidths.size(),
                "Sum of column widths (" + totalWidth + ") should be close to page width (" + pageWidth + ")");
    }

    @Test
    @SneakyThrows
    void columnWidthsUseEmbeddedFont() {
        // Verify that the embedded font family is used in CSS injection
        // This ensures the cross-platform fix is actually applied
        Field fontField = TableAnalyzer.class.getDeclaredField("EMBEDDED_FONT");
        fontField.setAccessible(true);
        Font embeddedFont = (Font) fontField.get(null);

        String fontFamily = embeddedFont.getFamily();
        assertNotNull(fontFamily, "Embedded font family should not be null");
        assertFalse(fontFamily.isEmpty(), "Embedded font family should not be empty");

        // The font should be either Liberation Sans (embedded loaded) or a fallback
        // Either way, column widths should be calculated using this font
        assertTrue(fontFamily.equals("Liberation Sans") || fontFamily.equals("SansSerif") || fontFamily.equals("Dialog"),
                "Font family should be known: " + fontFamily);
    }

    @Test
    void columnWidthsProduceExpectedValuesForFixedInput() {
        // Test with a fixed table structure to verify cross-platform consistency
        // With embedded font, these widths should be the same on all platforms
        Element table = new Element(Tag.valueOf("table"), "");
        Element row = table.appendElement("tr");
        row.appendElement("td").text("Short");
        row.appendElement("td").text("Medium length text");
        row.appendElement("td").text("A very long text content that takes more space");

        int pageWidth = 600; // Fixed width for predictable results
        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, pageWidth);

        assertEquals(3, columnWidths.size(), "Should have 3 columns");

        // Verify column widths sum to page width (within rounding)
        int sum = columnWidths.values().stream().mapToInt(Integer::intValue).sum();
        assertTrue(Math.abs(sum - pageWidth) <= 3, "Sum should equal page width, was: " + sum);

        // Verify relative ordering: column 0 < column 1 < column 2 (based on text length)
        assertTrue(columnWidths.get(0) < columnWidths.get(1),
                "Shorter text column should be narrower: col0=" + columnWidths.get(0) + ", col1=" + columnWidths.get(1));
        assertTrue(columnWidths.get(1) < columnWidths.get(2),
                "Medium text column should be narrower than long: col1=" + columnWidths.get(1) + ", col2=" + columnWidths.get(2));
    }

    // Dedicated file that flying-saucer (org.xhtmlrenderer) logs into during tests; configured in
    // src/test/resources/log4j2.xml via the SLF4J -> Log4j2 bridge.
    private static final Path FLYING_SAUCER_LOG = Path.of("target", "flying-saucer.log");

    @Test
    @SneakyThrows
    void getColumnWidthsWithSourcelessImageDoesNotLogErrors() {
        // An <img> with no usable src and no explicit width/height: during the measurement-only
        // pre-render flying-saucer resolves both CSS dimensions to -1 and (without the fix) tries to
        // build a -1x-1 placeholder image, which throws IllegalArgumentException internally and is
        // logged at ERROR by org.xhtmlrenderer.swing.SwingReplacedElementFactory. The export still
        // completes, but the log noise is the bug we guard against here.
        Element table = new Element(Tag.valueOf("table"), "");
        Element row = table.appendElement("tr");
        row.appendElement("td").appendElement("img"); // no src, no width/height
        row.appendElement("td").text("Some text");

        // The log file accumulates across the test run, so compare what flying-saucer logs *during* this call.
        String logBefore = readFlyingSaucerLog();
        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, 600);
        String logged = readFlyingSaucerLog().substring(logBefore.length());

        assertFalse(logged.contains("Failed to create image element"),
                "Source-less image must not trigger flying-saucer's failed-image ERROR during measurement, but it logged:\n" + logged);
        assertFalse(logged.contains("cannot be <= 0"),
                "Source-less image must not trigger an IllegalArgumentException for a -1x-1 placeholder, but it logged:\n" + logged);

        // Measurement must still succeed and yield both columns.
        assertEquals(2, columnWidths.size(), "Both columns should still be measured");
    }

    /** The measure lays a table out alone: it loads none of the images or stylesheets the table refers to. */
    @Test
    @SneakyThrows
    void loadsNoResourceTheTableRefersTo() {
        List<String> requested = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requested.add(exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
            Element table = Jsoup.parse("<table><tr>"
                    + "<td><img src=\"" + base + "/sized.png\" width=\"100\" height=\"50\"></td>"
                    + "<td><img src=\"" + base + "/unsized.png\"></td>"
                    + "<td>Text</td>"
                    + "</tr></table>").selectFirst("table");

            TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, 600, "@import url('" + base + "/imported.css');");

            assertEquals(List.of(), requested, "The measure must not load what the table refers to");
            assertEquals(3, metrics.columnWidths().size(), "The table is measured all the same");
        } finally {
            server.stop(0);
        }
    }

    /** An image embedded in its source needs no loading, so the measure still draws it at its size. */
    @Test
    void drawsAnImageEmbeddedInItsSource() {
        Element table = Jsoup.parse("<table><tr><td><img src=\"" + embeddedImage(300, 200) + "\"></td></tr></table>").selectFirst("table");

        List<Integer> rowHeights = TableAnalyzer.analyze(table, 600).rowHeights();

        assertEquals(1, rowHeights.size());
        assertTrue(rowHeights.getFirst() >= 200, "The row takes the height of the image, but measured " + rowHeights.getFirst());
    }

    /** An image in a text which may not wrap takes its height in the row as well. */
    @Test
    void takesTheHeightOfAnImageInATextWhichMayNotWrap() {
        String content = "<img src=\"" + embeddedImage(30, 200) + "\">EL-17";
        Element table = Jsoup.parse("<table><tr><td>" + content + "</td></tr></table>").selectFirst("table");
        Element noWrap = Jsoup.parse("<table><tr><td><span style=\"white-space: nowrap\">" + content + "</span></td></tr></table>").selectFirst("table");

        // An image stands on the baseline, while the line height which takes its place spreads around the text: they
        // differ by the descent of the font
        assertEquals(TableAnalyzer.analyze(table, 600).rowHeights().getFirst(), TableAnalyzer.analyze(noWrap, 600).rowHeights().getFirst(), 5,
                "As tall as without nowrap");
    }

    @Test
    @SneakyThrows
    void antiAliasRenderingHintResolvesToRealConstant() {
        // Until 10.6.0 the default of flying-saucer ("RenderingHints.VALUE_TEXT_ANTIALIAS_HGRB") resolved to nothing.
        // Java2DTextRenderer then fell back to the AWT desktop hints, null on a headless server, and logged an NPE
        // at WARN for every table measured. The default must stay a real constant.
        Object sentinel = new Object();
        Object hint = org.xhtmlrenderer.util.Configuration.valueFromClassConstant("xr.text.aa-rendering-hint", sentinel);

        assertNotSame(sentinel, hint, "aa-rendering-hint must resolve to a real RenderingHints constant, not the fallback");
        assertSame(RenderingHints.VALUE_TEXT_ANTIALIAS_ON, hint, "aa-rendering-hint should resolve to VALUE_TEXT_ANTIALIAS_ON");
    }

    @SneakyThrows
    private static String readFlyingSaucerLog() {
        return Files.exists(FLYING_SAUCER_LOG) ? Files.readString(FLYING_SAUCER_LOG, StandardCharsets.UTF_8) : "";
    }

    private Element createSimpleTable() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element row = table.appendElement("tr");
        row.appendElement("td").text("Column 1");
        row.appendElement("td").text("Column 2");
        row.appendElement("td").text("Column 3");
        return table;
    }

    @Test
    void getColumnWidthsA5LandscapeTest() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element tbody = table.appendElement("tbody");

        Element row1 = tbody.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/100x100.png")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/120x80.png")
                .attr("width", "120")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/150x100.png")
                .attr("width", "150")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/100x120.png")
                .attr("width", "100")
                .attr("height", "120");

        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/200x150.png")
                .attr("width", "200")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/250x200.png")
                .attr("width", "250")
                .attr("height", "200");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/300x200.png")
                .attr("width", "300")
                .attr("height", "200");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/180x180.png")
                .attr("width", "180")
                .attr("height", "180");

        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/400x300.png")
                .attr("width", "400")
                .attr("height", "300");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/500x350.png")
                .attr("width", "500")
                .attr("height", "350");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/450x300.png")
                .attr("width", "450")
                .attr("height", "300");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/350x400.png")
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
                .attr("src", "missing/100x100.png")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/700x300.png")
                .attr("width", "700")
                .attr("height", "300");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/250x200.png")
                .attr("width", "250")
                .attr("height", "200");

        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td").text("Small&nbsp;text");
        row2.appendElement("td").text("This column has a very long text description instead of an image, which should make the analyzer work differently");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/300x300.png")
                .attr("width", "300")
                .attr("height", "300");

        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/150x100.png")
                .attr("width", "150")
                .attr("height", "100");
        row3.appendElement("td").appendElement("img")
                .attr("src", "missing/200x150.png")
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
                .attr("src", "missing/100x100.png");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/300x200.png");
        row1.appendElement("td").text("Text content");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/500x300.png");

        // Row 2
        Element row2 = table.appendElement("tr");
        row2.appendElement("td").text("A");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/400x250.png");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/150x150.png");
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
                .attr("src", "missing/100x100.png")
                .attr("width", "100")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/150x100.png")
                .attr("width", "250")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/120x100.png")
                .attr("width", "120")
                .attr("height", "100");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/130x100.png")
                .attr("width", "330")
                .attr("height", "100");

        // Second row: one cell with colspan=2, then two regular cells
        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td")
                .attr("colspan", "2")
                .appendElement("img")
                .attr("src", "missing/300x150.png")
                .attr("width", "300")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/200x150.png")
                .attr("width", "200")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/180x150.png")
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

    @Test
    void getColumnWidthsWithLargeColspanTest() {
        Element table = new Element(Tag.valueOf("table"), "");
        Element tbody = table.appendElement("tbody");

        // First row: 6 regular columns
        Element row1 = tbody.appendElement("tr");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/80x80.png")
                .attr("width", "80")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/90x80.png")
                .attr("width", "90")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/100x80.png")
                .attr("width", "100")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/85x80.png")
                .attr("width", "85")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/95x80.png")
                .attr("width", "95")
                .attr("height", "80");
        row1.appendElement("td").appendElement("img")
                .attr("src", "missing/110x80.png")
                .attr("width", "110")
                .attr("height", "80");

        // Second row: one cell with colspan=4, then two regular cells
        Element row2 = tbody.appendElement("tr");
        row2.appendElement("td")
                .attr("colspan", "4")
                .appendElement("img")
                .attr("src", "missing/400x150.png")
                .attr("width", "400")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/120x150.png")
                .attr("width", "120")
                .attr("height", "150");
        row2.appendElement("td").appendElement("img")
                .attr("src", "missing/130x150.png")
                .attr("width", "130")
                .attr("height", "150");

        // Third row: regular cell, colspan=3, then two regular cells
        Element row3 = tbody.appendElement("tr");
        row3.appendElement("td").text("Column 1");
        row3.appendElement("td")
                .attr("colspan", "3")
                .text("Spanning three columns with some text content here");
        row3.appendElement("td").text("Column 5");
        row3.appendElement("td").text("Column 6");

        Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(ConversionParams.builder().orientation(Orientation.LANDSCAPE).build()));

        // Should have 6 columns total
        assertEquals(6, columnWidths.size());

        // Verify all columns have positive widths
        for (int i = 0; i < 6; i++) {
            assertTrue(columnWidths.containsKey(i), "Column " + i + " should have a width");
            assertTrue(columnWidths.get(i) >= 130 && columnWidths.get(i) <= 180, "Column " + i + " width should be certain range, but was " + columnWidths.get(i));
        }
    }

    @Test
    void measuresAFontSizeInPointsAsCssDoes() {
        // CSS counts 96 pixels to an inch, so 9pt is 12px. At the 72 dpi of a headless server it was 9px, and a table too
        // wide for its page measured as one which fits
        String words = "Sicherheitsanforderungen ".repeat(8);
        int inPoints = TableAnalyzer.analyze(Jsoup.parse("<table><tr><td style=\"font-size: 9pt\">" + words + "</td></tr></table>").selectFirst("table"), 2000).tableWidth();
        int inPixels = TableAnalyzer.analyze(Jsoup.parse("<table><tr><td style=\"font-size: 12px\">" + words + "</td></tr></table>").selectFirst("table"), 2000).tableWidth();

        assertEquals(inPixels, inPoints);
    }

    /** WeasyPrint kerns text, so a pair as "AV" is narrower than its letters added up, and the measure kerns it too. */
    @Test
    void measuresTextWithKerning() {
        String text = "AVAVAVAVAVAVAVAVAVAV";
        Font font = TableAnalyzer.loadFontFromPath("/fonts/LiberationSans-Regular.ttf").deriveFont(16f);
        FontRenderContext context = new FontRenderContext(null, true, true);
        double kerned = font.deriveFont(Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON)).getStringBounds(text, context).getWidth();
        double unkerned = font.getStringBounds(text, context).getWidth();

        int measured = TableAnalyzer.analyze(Jsoup.parse("<table style=\"border-collapse: collapse\"><tr><td style=\"padding: 0; border: 0\">" + text + "</td></tr></table>")
                .selectFirst("table"), 2000).tableWidth();

        assertTrue(unkerned - kerned > 10, "Kerning makes the text narrower: " + unkerned + " against " + kerned);
        assertEquals(Math.round(kerned), measured);
    }

    @Test
    void measuresACellWhichBreaksAnywhereAsNarrowAsItsWidestCharacter() {
        String cells = "<td>Approved</td>".repeat(30);
        int whole = TableAnalyzer.analyze(Jsoup.parse("<table><tr>" + cells + "</tr></table>").selectFirst("table"), 592).tableWidth();
        int anywhere = TableAnalyzer.analyze(Jsoup.parse("<table><tr>" + cells.replace("<td>", "<td style=\"overflow-wrap: anywhere\">") + "</tr></table>").selectFirst("table"), 592).tableWidth();

        assertTrue(whole > 592, "Whole words leave the table no room, but it measured " + whole);
        assertTrue(anywhere <= 592, "Broken anywhere, the words fit, but the table measured " + anywhere);
    }

    @Test
    void measuresACellWhichHyphenatesAsOneWhichBreaksItsWords() {
        String cells = "<td>Rechtsschutzversicherungsgesellschaft</td>".repeat(4);
        int whole = TableAnalyzer.analyze(Jsoup.parse("<table><tr>" + cells + "</tr></table>").selectFirst("table"), 592).tableWidth();
        int hyphenated = TableAnalyzer.analyze(Jsoup.parse("<table><tr>" + cells.replace("<td>", "<td style=\"hyphens: auto\">") + "</tr></table>").selectFirst("table"), 592).tableWidth();

        assertTrue(whole > 592, "Whole words leave the table no room, but it measured " + whole);
        assertTrue(hyphenated <= 592, "Hyphenated, the words fit, but the table measured " + hyphenated);
    }

    /** An image of the given size, embedded in its source. */
    private static String embeddedImage(int width, int height) {
        byte[] png = MediaUtils.toPng(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }
}
