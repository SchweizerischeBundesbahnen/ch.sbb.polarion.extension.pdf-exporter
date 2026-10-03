package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

class ExportStylesheetTest {

    @Test
    void leavesAnEmbeddedResourceOut() {
        CascadingStyleSheet stylesheet = ExportStylesheet.read("""
                @font-face { font-family: 'A'; src: url(data:font/woff2;base64,d09GMgABAAAAA+/=) format('woff2'); }
                @font-face { font-family: 'B'; src: url("data:font/ttf;base64,AAEAAAAS"); }
                th { background: URL( 'data:image/png;base64,iVBORw0KGgo=' ) no-repeat; padding: 4px; }""");

        assertThat(stylesheet).isNotNull();
        String css = new CSSWriter(new CSSWriterSettings()).getCSSAsString(stylesheet);
        assertThat(css).doesNotContain("base64").contains("url()").contains("padding:4px");
        assertThat(stylesheet.getAllFontFaceRules()).hasSize(2);
    }

    @Test
    void keepsAResourceItLinks() {
        CascadingStyleSheet stylesheet = ExportStylesheet.read("th { background: url('/polarion/ria/images/data.png'); }");

        assertThat(stylesheet).isNotNull();
        assertThat(new CSSWriter(new CSSWriterSettings()).getCSSAsString(stylesheet)).contains("/polarion/ria/images/data.png");
    }

    /** The export embeds its fonts, megabytes of the CSS, which change nothing the layout reads. */
    @Test
    @SneakyThrows
    void readsTheSameLayoutFromTheCssWithItsFontsEmbedded() {
        try (InputStream stream = ExportStylesheetTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            String css = new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8) + "th { padding: 12px; }";
            String font = "url(data:font/woff2;base64," + "d09GMgABAAAAA".repeat(1_000) + ")";
            String embedded = css.replaceAll("url\\([^)]*\\)", Matcher.quoteReplacement(font));

            assertThat(embedded).contains("data:font/woff2");
            assertThat(PageLayout.of(embedded)).isEqualTo(PageLayout.of(css));
        }
    }
}
