package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HyphenationRulesTest {

    @Test
    void findsTheSelectorsOfTheRulesWhichTurnHyphenationOff() {
        String css = """
                h1, h2 { hyphens: none; }
                .content { hyphens: auto; }
                @media print { table.no-split td { -webkit-hyphens: MANUAL; } }
                p { color: red; }
                """;

        assertThat(HyphenationRules.turningHyphenationOff(css)).containsExactly("h1", "h2", "table.no-split td");
    }

    @Test
    void findsNoneInAStylesheetWhichLetsTextHyphenate() {
        assertThat(HyphenationRules.turningHyphenationOff(".content { hyphens: auto; } table { width: 100%; }")).isEmpty();
    }
}
