package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FontFamilyUseTest {

    @Test
    void findsAFamilyARuleNames() {
        FontFamilyUse use = FontFamilyUse.in("body { font-family: Arial, \"Open Sans\", sans-serif; }");

        assertThat(use.test("Open Sans")).isTrue();
        assertThat(use.test("open  sans")).as("in any case, its words apart by any space").isTrue();
        assertThat(use.test("Arial")).isTrue();
        assertThat(use.test("Selawik")).isFalse();
    }

    @Test
    void findsAFamilyACustomPropertyOrAStyleAttributeNames() {
        FontFamilyUse use = FontFamilyUse.in(
                ".fa { font-family: var(--fa-style-family, \"Font Awesome 6 Free\"); }",
                "<span style=\"font-family: 'Font Awesome 6 Brands'\">");

        assertThat(use.test("Font Awesome 6 Free")).isTrue();
        assertThat(use.test("Font Awesome 6 Brands")).isTrue();
        assertThat(use.test("Font Awesome 5 Free")).isFalse();
    }

    /** A name which says nothing of the use of a family: in the rule which declares it, a comment, a path or a word. */
    @ParameterizedTest
    @ValueSource(strings = {
            "@font-face { font-family: \"FontAwesome\"; src: url(fa.ttf); } .x { color: red; }",
            "/*! Font Awesome Free 6.2.0 by @fontawesome, FontAwesome */ .x { color: red; }",
            "<link href=\"/polarion/ria/fontawesome-6.2.0/css/all.min.css\"/> .myfontawesome { }",
            "<img src=\"data:image/png;base64,iVBORw0FontAwesome+gg==\"/>"
    })
    void doesNotCountANameWhichNamesNoUse(String text) {
        assertThat(FontFamilyUse.in(text).test("FontAwesome")).isFalse();
    }
}
