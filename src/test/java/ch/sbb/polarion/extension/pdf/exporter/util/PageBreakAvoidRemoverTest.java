package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.TestStringUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PageBreakAvoidRemoverTest {

    private PageBreakAvoidRemover remover;

    @BeforeEach
    void init() {
        remover = new PageBreakAvoidRemover();
    }

    @Test
    @SneakyThrows
    void removePageBreakAvoidsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/withPageBreakAvoids.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/withoutPageBreakAvoids.html")) {
            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = remover.removePageBreakAvoids(invalidHtml);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }
}
