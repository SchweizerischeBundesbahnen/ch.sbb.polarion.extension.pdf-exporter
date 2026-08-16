package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class ExternalCssInternalizerTest {
    @Mock
    private FileResourceProvider fileResourceProvider;

    @InjectMocks
    private ExternalCssInternalizer cssLinkInliner;


    @Test
    void shouldReturnEmptyResultForUnknownTags() {
        Optional<String> result = cssLinkInliner.inline(Map.of("rel", "unknown"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldConvertStylesheetLink() {
        when(fileResourceProvider.getResourceAsBytes("my-href-location")).thenReturn("test-stylesheet".getBytes());
        Optional<String> result = cssLinkInliner.inline(Map.of("rel", "stylesheet", "href", "my-href-location"));

        assertNotNull(result);
        assertEquals("<style>test-stylesheet</style>", result.get());
    }

    @Test
    void shouldConvertStylesheetLinkAndTransferDataPrecedence() {
        when(fileResourceProvider.getResourceAsBytes("my-href-location")).thenReturn("test-stylesheet".getBytes());
        Optional<String> result = cssLinkInliner.inline(Map.of(
                "rel", "stylesheet",
                "href", "my-href-location",
                "data-precedence", "test-data-precedence"));

        assertNotNull(result);
        assertEquals("<style data-precedence=\"test-data-precedence\">test-stylesheet</style>", result.get());
    }

    @Test
    void shouldConvertStylesheetLinkAndProcessRelativeLinks() {
        when(fileResourceProvider.getResourceAsBytes("/some/location/file.css")).thenReturn("""
                @font-face {
                  src: url('../fonts/some-font.woff');
                }
                @font-face {
                  src: url('relative/quotes/some-font2.woff');
                }
                @font-face {
                  src: url("relative/double/quotes/some-font3.woff");
                }
                @font-face {
                  src: url(relative/no/quotes/some-font4.woff);
                }
                @font-face {
                  src: url('/non-relative/fonts/some-font3.woff');
                }
                """.getBytes());
        Optional<String> result = cssLinkInliner.inline(Map.of(
                "rel", "stylesheet",
                "href", "/some/location/file.css",
                "data-precedence", "test-data-precedence"));

        assertThat(result).isNotEmpty();
        assertThat(result.get()).contains(
                "src: url(/some/location/../fonts/some-font.woff)",
                "src: url(/some/location/relative/quotes/some-font2.woff)",
                "src: url(/some/location/relative/double/quotes/some-font3.woff)",
                "src: url(/some/location/relative/no/quotes/some-font4.woff)",
                "src: url('/non-relative/fonts/some-font3.woff')"
        );
    }
    @ParameterizedTest
    @ValueSource(strings = {"stylesheet", "Stylesheet", "STYLESHEET", "stylesheet ", " stylesheet"})
    void internalizesEverySpellingOfTheRelAStylesheetHas(String rel) {
        // a renderer reads rel as a list of tokens, each case insensitive, and loads all of these
        when(fileResourceProvider.getResourceAsBytes("my-href-location")).thenReturn("test-stylesheet".getBytes());

        Optional<String> result = cssLinkInliner.inline(Map.of("rel", rel, "href", "my-href-location"));

        assertThat(result).contains("<style>test-stylesheet</style>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"icon", "preload", "stylesheets", "nostylesheet",
            // an alternative style sheet is skipped by a renderer until someone selects it, and nothing
            // selects one in an export: measured, weasyprint-service does not ask for such a link
            "alternate stylesheet", "stylesheet alternate", "Alternate Stylesheet"})
    void keepsALinkWhichNamesNoStylesheet(String rel) {
        Optional<String> result = cssLinkInliner.inline(Map.of("rel", rel, "href", "my-href-location"));

        assertThat(result).isEmpty();
    }
    @Test
    void shouldKeepAValueOfTheDocumentInsideItsAttribute() {
        // the value is written back as markup, so a quote in it may not end the attribute it sits in
        when(fileResourceProvider.getResourceAsBytes("my-href-location")).thenReturn("body{color:red}".getBytes());

        Optional<String> result = cssLinkInliner.inline(Map.of(
                "rel", "stylesheet",
                "href", "my-href-location",
                "data-precedence", "x\"><img src=http://169.254.169.254/x>"));

        assertThat(result).isPresent();
        assertThat(result.get())
                .doesNotContain("<img src=http://169.254.169.254/x>")
                .contains("&quot;&gt;&lt;img");
    }

    @Test
    void shouldKeepAStylesheetInsideItsStyleElement() {
        // a style element ends at the first closing tag written in it, and a stylesheet may carry one
        when(fileResourceProvider.getResourceAsBytes("my-href-location"))
                .thenReturn("a::after{content:\"</style><img src=http://169.254.169.254/x>\"}".getBytes());

        Optional<String> result = cssLinkInliner.inline(Map.of("rel", "stylesheet", "href", "my-href-location"));

        assertThat(result).isPresent();
        assertThat(result.get())
                .doesNotContain("</style><img")
                .endsWith("</style>");
    }
}
