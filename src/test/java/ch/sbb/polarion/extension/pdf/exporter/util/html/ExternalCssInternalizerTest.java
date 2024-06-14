package ch.sbb.polarion.extension.pdf.exporter.util.html;

import ch.sbb.polarion.extension.pdf.exporter.util.FileResourceProvider;
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
}