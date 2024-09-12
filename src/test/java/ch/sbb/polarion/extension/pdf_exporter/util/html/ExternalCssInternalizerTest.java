package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

        assertThat(result).isNotEmpty();
        assertThat(result.get()).isEqualTo("<style>test-stylesheet</style>");
    }

    @Test
    void shouldConvertStylesheetLinkAndTransferDataPrecedence() {
        when(fileResourceProvider.getResourceAsBytes("my-href-location")).thenReturn("test-stylesheet".getBytes());
        Optional<String> result = cssLinkInliner.inline(Map.of(
                "rel", "stylesheet",
                "href", "my-href-location",
                "data-precedence", "test-data-precedence"));

        assertThat(result).isNotEmpty();
        assertThat(result.get()).isEqualTo("""
                <style data-precedence="test-data-precedence">test-stylesheet</style>""");
    }

    @Test
    void shouldConvertStylesheetLinkAndProcessRelativeLinks() {
        when(fileResourceProvider.getResourceAsBytes("/some/location/file.css")).thenReturn("""
                @font-face {
                  src: url('../fonts/some-font.woff');
                }
                @font-face {
                  src: url('relative2/some-font2.woff');
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
                "src: url(/some/location/relative2/some-font2.woff)",
                "src: url('/non-relative/fonts/some-font3.woff')"
        );
    }
}