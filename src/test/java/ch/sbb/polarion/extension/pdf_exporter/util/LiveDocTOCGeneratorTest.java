package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import lombok.SneakyThrows;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
