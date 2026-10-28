package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class BodyRulesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // WeasyPrint gives the body a margin of 8px on each side
            "p { margin: 0; }                                         | 16",
            "body { margin-top: 0; margin-bottom: 0; }                | 16",
            "body { margin: 0; }                                      | 0",
            "body { margin: 0 20px; }                                 | 40",
            "body { margin: 0 20px 0 10px; }                          | 30",
            "body { margin: 0; padding: 0 5px 0 15px; }               | 20",
            "body { margin-left: 1cm; margin-right: 0; padding-right: 10pt; } | 51",
            "body { margin: 0; } body { margin-left: 4px; }           | 4"
    })
    void takesTheSideMarginsAndPaddingsOfTheBody(String css, int sides) {
        assertThat(BodyRules.sides(css)).isEqualTo(sides);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "html body { margin: 0 100px; }        | 200",
            "html > body { margin: 0; }            | 0",
            ":root body { margin: 0 30px; }        | 60",
            "body.watermark { margin: 0 50px; }    | 16",
            "body p { margin: 0 50px; }            | 16"
    })
    void takesTheRulesWhoseSelectorEndsInTheBody(String css, int sides) {
        assertThat(BodyRules.sides(css)).isEqualTo(sides);
    }

    @Test
    void keepsAnImportantDeclarationOverALaterOne() {
        assertThat(BodyRules.sides("body { margin: 0 100px !important; } body { margin: 0; }")).isEqualTo(200);
        assertThat(BodyRules.sides("body { margin: 0 100px !important; } body { margin-left: 0 !important; }")).isEqualTo(100);
        assertThat(BodyRules.sides("body { padding-right: 10px !important; } body { padding: 0; margin: 0; }")).isEqualTo(10);
    }

    @Test
    void takesTheRulesForPrint() {
        assertThat(BodyRules.sides("body { margin: 0; } @media print { body { padding-right: 10px; } }")).isEqualTo(10);
        assertThat(BodyRules.sides("body { margin: 0; } @media all { body { padding-right: 10px; } }")).isEqualTo(10);
        assertThat(BodyRules.sides("body { margin: 0; } @media not screen { body { padding-right: 100px; } }")).isEqualTo(100);
    }

    @Test
    void leavesTheRulesForTheScreenOut() {
        assertThat(BodyRules.sides("body { margin: 0; } @media screen { body { padding: 50px; } }")).isZero();
        assertThat(BodyRules.sides("body { margin: 0; } @media not print { body { padding: 50px; } }")).isZero();
    }

    @Test
    void keepsTheMarginWhereTheCssStatesItInARelativeUnit() {
        assertThat(BodyRules.sides("body { margin: 0 5%; }")).isEqualTo(16);
        assertThat(BodyRules.sides("body { margin: 0; padding: 0 2em; }")).isZero();
    }

    @Test
    @SneakyThrows
    void readsTheDefaultCss() {
        try (InputStream css = BodyRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            // The margins of 8px WeasyPrint gives the body, and the padding of 10px the default CSS gives it on the right
            assertThat(BodyRules.sides(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8))).isEqualTo(26);
        }
    }
}
