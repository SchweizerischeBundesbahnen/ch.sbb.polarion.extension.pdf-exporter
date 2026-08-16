package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class HtmlLinksHelperTest {
    @Mock
    private LinkInternalizer linkInternalizer1;
    @Mock
    private LinkInternalizer linkInternalizer2;

    private HtmlLinksHelper htmlLinksHelper;

    @BeforeEach
    void setup() {
        htmlLinksHelper = new HtmlLinksHelper(Set.of(linkInternalizer1, linkInternalizer2));
    }

    @Test
    void shouldCallInlinersForLinkTags() {
        htmlLinksHelper.internalizeLinks("""
                <html lang='en'><head><link>some content<link></head>""");

        Stream.of(linkInternalizer1, linkInternalizer2).forEach(inliner -> verify(inliner, times(2)).inline(Map.of()));
    }

    @Test
    void shouldParseAttributesAndReplaceLinkTags() {
        when(linkInternalizer1.inline(anyMap())).thenReturn(Optional.of("<style>replacement</style>"));
        String resultHtml = htmlLinksHelper.internalizeLinks("""
                <html lang='en'><head><link attr1="value1" ATTR2="value2">some content</head>""");

        assertThat(resultHtml).isEqualTo("""
                <html lang='en'><head><style>replacement</style>some content</head>""");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(linkInternalizer1).inline(captor.capture());
        assertThat(captor.getValue()).containsExactly(Map.entry("attr1", "value1"), Map.entry("attr2", "value2")); // also it must lowercase attributes
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // a renderer reads each of these as one link, so each of them reaches an inliner here
            "<link rel=stylesheet href=http://169.254.169.254/x.css>",
            "<link rel='stylesheet' href=http://169.254.169.254/x.css>",
            "<link rel=\"stylesheet\" href=\"http://169.254.169.254/x.css?a=>b\">",
            "<link REL=Stylesheet HREF='http://169.254.169.254/x.css'>",
            "<link\n   rel = 'stylesheet'\n   href = 'http://169.254.169.254/x.css'>"
    })
    void shouldReadALinkWhateverItsAttributesLookLike(String linkTag) {
        when(linkInternalizer1.inline(anyMap())).thenReturn(Optional.of("<style>replacement</style>"));

        String resultHtml = htmlLinksHelper.internalizeLinks("<html lang='en'><head>" + linkTag + "</head>");

        assertThat(resultHtml)
                .isEqualTo("<html lang='en'><head><style>replacement</style></head>")
                .doesNotContain("169.254.169.254");
    }

    @Test
    void shouldReadTheAddressOfALinkWithoutQuotes() {
        Map<String, String> attributes = HtmlLinksHelper.parseLinkTagAttributes(
                "<link rel=stylesheet href=http://169.254.169.254/x.css>");

        assertThat(attributes).containsExactly(
                Map.entry("rel", "stylesheet"), Map.entry("href", "http://169.254.169.254/x.css"));
    }

    @Test
    void shouldReplaceLinksWhereTheyStandWhateverTheTreeOrderIs() {
        // a link written inside a table is moved before it by the parser, so the element the parser
        // lists first is the one written second: the replacements have to follow the text
        when(linkInternalizer1.inline(anyMap())).thenAnswer(invocation -> {
            Map<?, ?> attributes = invocation.getArgument(0);
            return Optional.of("<style>" + attributes.get("href") + " inlined, and longer than the tag was</style>");
        });

        String resultHtml = htmlLinksHelper.internalizeLinks(
                "<table><tr><td><link rel='stylesheet' href='a.css'></td><link rel='stylesheet' href='b.css'></tr></table>");

        assertThat(resultHtml)
                .contains("<style>a.css inlined, and longer than the tag was</style>")
                .contains("<style>b.css inlined, and longer than the tag was</style>")
                .doesNotContain("<link");
    }
}
