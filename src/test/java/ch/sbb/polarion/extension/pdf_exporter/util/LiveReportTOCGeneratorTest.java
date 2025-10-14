package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveReportTOCGeneratorTest {

    @Test
    @SneakyThrows
    void tableOfContent() {
        try (
                InputStream isInitialHtml = this.getClass().getResourceAsStream("/tableOfContentLiveReportBeforeProcessingFormatted.html");
                InputStream isExpectedHtml = this.getClass().getResourceAsStream("/tableOfContentLiveReportAfterProcessing.html")
        ) {
            String initialHtml = new String(isInitialHtml.readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(isExpectedHtml.readAllBytes(), StandardCharsets.UTF_8);

            LiveReportTOCGenerator liveReportTOCGenerator = new LiveReportTOCGenerator();
            String processedHtml = liveReportTOCGenerator.addTableOfContent(initialHtml);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
        }
    }

}
