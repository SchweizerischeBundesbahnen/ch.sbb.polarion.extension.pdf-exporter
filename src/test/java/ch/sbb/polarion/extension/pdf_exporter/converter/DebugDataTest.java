package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.pdf_exporter.model.DebugData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DebugDataTest {

    @Test
    void shouldBuildDebugDataWithAllFields() {
        Instant now = Instant.now();
        DebugData debugData = DebugData.builder()
                .originalHtml("<html>original</html>")
                .processedHtml("<html>processed</html>")
                .timingReport("timing report")
                .user("testUser")
                .createdAt(now)
                .documentTitle("Test Document")
                .build();

        assertThat(debugData.originalHtml()).isEqualTo("<html>original</html>");
        assertThat(debugData.processedHtml()).isEqualTo("<html>processed</html>");
        assertThat(debugData.timingReport()).isEqualTo("timing report");
        assertThat(debugData.user()).isEqualTo("testUser");
        assertThat(debugData.createdAt()).isEqualTo(now);
        assertThat(debugData.documentTitle()).isEqualTo("Test Document");
    }

    @Test
    void shouldBuildDebugDataWithNullableFields() {
        Instant now = Instant.now();
        DebugData debugData = DebugData.builder()
                .user("testUser")
                .createdAt(now)
                .build();

        assertThat(debugData.originalHtml()).isNull();
        assertThat(debugData.processedHtml()).isNull();
        assertThat(debugData.timingReport()).isNull();
        assertThat(debugData.documentTitle()).isNull();
    }

    @Test
    void hasContent_shouldReturnTrueWhenOriginalHtmlPresent() {
        DebugData debugData = DebugData.builder()
                .originalHtml("<html>content</html>")
                .user("testUser")
                .createdAt(Instant.now())
                .build();

        assertThat(debugData.hasContent()).isTrue();
    }

    @Test
    void hasContent_shouldReturnTrueWhenProcessedHtmlPresent() {
        DebugData debugData = DebugData.builder()
                .processedHtml("<html>content</html>")
                .user("testUser")
                .createdAt(Instant.now())
                .build();

        assertThat(debugData.hasContent()).isTrue();
    }

    @Test
    void hasContent_shouldReturnTrueWhenTimingReportPresent() {
        DebugData debugData = DebugData.builder()
                .timingReport("timing report")
                .user("testUser")
                .createdAt(Instant.now())
                .build();

        assertThat(debugData.hasContent()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            ",,,false",
            "'','','',false",
            "content,,, true",
            ",content,, true",
            ",,content, true"
    })
    void hasContent_variousCombinations(String original, String processed, String timing, boolean expected) {
        DebugData debugData = DebugData.builder()
                .originalHtml(original)
                .processedHtml(processed)
                .timingReport(timing)
                .user("testUser")
                .createdAt(Instant.now())
                .build();

        assertThat(debugData.hasContent()).isEqualTo(expected);
    }
}
