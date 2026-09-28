package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import lombok.SneakyThrows;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveDocTOCGeneratorTest {

    @Test
    @SneakyThrows
    void tableOfContent() {
        try (
                InputStream isInitialHtml = this.getClass().getResourceAsStream("/tableOfContentLiveDocBeforeProcessingFormatted.html");
                InputStream isExpectedHtml = this.getClass().getResourceAsStream("/tableOfContentLiveDocAfterProcessing.html")
        ) {
            String initialHtml = new String(isInitialHtml.readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(isExpectedHtml.readAllBytes(), StandardCharsets.UTF_8);

            LiveDocTOCGenerator liveDocTOCGenerator = new LiveDocTOCGenerator();

            Document document = JSoupUtils.parseHtml(initialHtml);

            liveDocTOCGenerator.addTableOfContent(document);
            String processedHtml = document.body().html();

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfContentWikiContent() {
        try (
                InputStream isInitialHtml = this.getClass().getResourceAsStream("/tableOfContentLiveDocWikiContentBeforeProcessingFormatted.html");
                InputStream isExpectedHtml = this.getClass().getResourceAsStream("/tableOfContentLiveDocWikiContentAfterProcessing.html")
        ) {
            String initialHtml = new String(isInitialHtml.readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(isExpectedHtml.readAllBytes(), StandardCharsets.UTF_8);

            LiveDocTOCGenerator liveDocTOCGenerator = new LiveDocTOCGenerator();

            Document document = JSoupUtils.parseHtml(initialHtml);

            liveDocTOCGenerator.addTableOfContent(document);
            String processedHtml = document.body().html();

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
        }
    }

    @Test
    void tableOfContentNestsALevelInsideItsItem() {
        // A <ul> beside an <li> is no list: the level took neither its indent nor the line height of the
        // table, which is the uneven spacing of #1075.
        Document document = JSoupUtils.parseHtml("""
                <pd4ml:toc></pd4ml:toc>
                <h1><a id="one"></a>1 One</h1>
                <h2><a id="one-one"></a>1.1 One one</h2>
                <h1><a id="two"></a>2 Two</h1>""");

        new LiveDocTOCGenerator().addTableOfContent(document);

        String html = document.body().html();
        assertTrue(document.select("ul.toc > ul").isEmpty(), html);
        assertEquals(1, document.select("ul.toc > li > ul > li").size(), html);
        assertEquals(2, document.select("ul.toc > li").size(), html);
    }

    @Test
    void tableOfContentDropsTheLineBreakBehindIt() {
        // Polarion writes a line break behind the table, which prints as a blank line the editor does not
        // show and the tables of figures and of tables do not carry; #1076.
        Document document = JSoupUtils.parseHtml("""
                <pd4ml:toc></pd4ml:toc><br/><p id="after">text<br/>and more</p>
                <h1><a id="one"></a>1 One</h1>""");

        new LiveDocTOCGenerator().addTableOfContent(document);

        String html = document.body().html();
        assertTrue(html.contains("</ul><p id=\"after\">"), html);
        // and a line break which belongs to the content stays
        assertEquals(1, document.select("p#after br").size(), html);
    }

    @Test
    void tableOfContentOverLevelsTheDocumentNames() {
        // The levels come from the document and may carry anything. A huge one built its heading selector
        // over the whole integer range, which never ended, and an unreadable one failed the export.
        Document document = JSoupUtils.parseHtml("""
                <pd4ml:toc tocInit="none" tocMax="2147483647"></pd4ml:toc>
                <h1><a id="chapter"></a>1 Chapter</h1>""");

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> new LiveDocTOCGenerator().addTableOfContent(document));

        String processedHtml = document.body().html();
        assertTrue(processedHtml.contains("<ul class=\"toc\">"), processedHtml);
        assertTrue(processedHtml.contains("#chapter"), processedHtml);
    }

    @Test
    @SneakyThrows
    void tableOfContentWithAngleBrackets() {
        try (
                InputStream isInitialHtml = this.getClass().getResourceAsStream("/tableOfContentWithAngleBracketsBeforeProcessing.html");
                InputStream isExpectedHtml = this.getClass().getResourceAsStream("/tableOfContentWithAngleBracketsAfterProcessing.html")
        ) {
            String initialHtml = new String(isInitialHtml.readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(isExpectedHtml.readAllBytes(), StandardCharsets.UTF_8);

            LiveDocTOCGenerator liveDocTOCGenerator = new LiveDocTOCGenerator();

            Document document = JSoupUtils.parseHtml(initialHtml);

            liveDocTOCGenerator.addTableOfContent(document);
            String processedHtml = document.body().html();

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
        }
    }
}
