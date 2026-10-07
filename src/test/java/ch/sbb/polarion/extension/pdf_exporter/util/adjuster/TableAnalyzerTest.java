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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
                    + "<td style=\"background-image: url('" + base + "/background.png')\">Text</td>"
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

    @Test
    @SneakyThrows
    void antiAliasRenderingHintResolvesToRealConstant() {
        // Force TableAnalyzer's static initializer to run, pinning xr.text.aa-rendering-hint before
        // flying-saucer's Configuration singleton is first read.
        Class.forName(TableAnalyzer.class.getName());

        // flying-saucer's own default ("RenderingHints.VALUE_TEXT_ANTIALIAS_HGRB") is unresolvable, so without
        // the fix this returns the sentinel and Java2DTextRenderer falls back to the AWT desktop hints - null on
        // a headless server - which throws an NPE logged at WARN for every renderer (one per table). Verify the
        // hint instead resolves to a real constant so that path is never taken.
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
