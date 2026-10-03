package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ParagraphRulesTest {

    @Test
    @SneakyThrows
    void readsTheRoomTheDefaultCssLeaves() {
        try (InputStream css = ParagraphRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            // A bottom margin of .8em of 10pt, 10.7 px, and the descent of the line, 4 px
            assertThat(ParagraphRules.roomUnderAnImage(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8))).isEqualTo(15);
        }
    }

    @Test
    void readsTheFontAndTheMarginsACssStates() {
        // 2em of 12pt is 32 px, and the descent of the line 4.8 px
        assertThat(ParagraphRules.roomUnderAnImage("body { font-size: 12pt; } p { margin: 0 0 2em; }")).isEqualTo(37);
        assertThat(ParagraphRules.roomUnderAnImage("body { font-size: 10pt; } p { margin-bottom: 20px; }")).isEqualTo(24);
    }

    @Test
    void takesTheLastMarginItStates() {
        assertThat(ParagraphRules.roomUnderAnImage("body { font-size: 10pt; } p { margin-bottom: 20px; } p { margin-bottom: 0; }")).isEqualTo(4);
    }

    @Test
    void leavesTheRoomOfTheDefaultCssWhereTheCssStatesNoMargin() {
        assertThat(ParagraphRules.roomUnderAnImage("body { font-size: 10pt; }")).isEqualTo(ParagraphRules.DEFAULT_ROOM_PX);
        assertThat(ParagraphRules.roomUnderAnImage("p { margin-bottom: 1rem; }")).isEqualTo(ParagraphRules.DEFAULT_ROOM_PX);
        assertThat(ParagraphRules.roomUnderAnImage("")).isEqualTo(ParagraphRules.DEFAULT_ROOM_PX);
    }
}
