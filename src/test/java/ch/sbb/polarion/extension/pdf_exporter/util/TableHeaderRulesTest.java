package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class TableHeaderRulesTest {

    @Test
    void takesWhatMakesAHeaderTaller() {
        String rules = TableHeaderRules.measuredBy("th { font-size: 20pt; padding: 24px !important; color: red; }");

        assertThat(rules).contains("th").contains("font-size:20pt").contains("padding:24px !important").doesNotContain("color");
    }

    @Test
    void takesAHeaderNamedInASelectorOfSeveral() {
        String rules = TableHeaderRules.measuredBy(".polarion-Document-table th, p { line-height: 2; } thead tr { border-top: 3px solid black; }");

        assertThat(rules).contains(".polarion-Document-table th").doesNotContain(", p").contains("line-height:2").contains("thead tr").contains("border-top");
    }

    @Test
    void leavesWhatDoesNotConcernAHeader() {
        assertThat(TableHeaderRules.measuredBy("td { padding: 24px; } p.thesis { font-size: 30pt; } th { background: #eee; }")).isEmpty();
    }

    @Test
    void leavesAValueWhichNamesAResource() {
        assertThat(TableHeaderRules.measuredBy("th { border: 1px solid; font: 12pt url(font.woff); }")).contains("border").doesNotContain("url(");
    }

    /** The default CSS says nothing which makes a header taller, so an export without a style package measures it once. */
    @Test
    @SneakyThrows
    void readsNothingOfTheDefaultCss() {
        try (InputStream css = TableHeaderRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            assertThat(TableHeaderRules.measuredBy(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8))).isEmpty();
        }
    }

    @Test
    void readsNothingOfCssWithoutAHeader() {
        assertThat(TableHeaderRules.measuredBy("body { margin: 0; }")).isEmpty();
        assertThat(TableHeaderRules.measuredBy("")).isEmpty();
    }
}
