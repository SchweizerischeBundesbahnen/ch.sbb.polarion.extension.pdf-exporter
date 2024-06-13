package ch.sbb.polarion.extension.pdf.exporter.util.html;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HtmlLinksHelperTest {
    @Mock
    private LinkInternalizer linkInternalizer1;
    @Mock
    private LinkInternalizer linkInternalizer2;

    @Mock
    private PdfExporterExtensionConfiguration pdfExporterExtensionConfiguration;
    private HtmlLinksHelper htmlLinksHelper;
    private MockedStatic<PdfExporterExtensionConfiguration> extensionConfigurationMockedStatic;

    @BeforeEach
    void setup() {
        extensionConfigurationMockedStatic = mockStatic(PdfExporterExtensionConfiguration.class);
        extensionConfigurationMockedStatic.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(pdfExporterExtensionConfiguration);
        when(pdfExporterExtensionConfiguration.getInternalizeExternalCss()).thenReturn(true);
        htmlLinksHelper = new HtmlLinksHelper(Set.of(linkInternalizer1, linkInternalizer2));
    }

    @AfterEach
    void shutdown() {
        extensionConfigurationMockedStatic.close();
    }

    @Test
    void shouldCallInlinersForLinkTags() {
        htmlLinksHelper.internalizeLinks("""
                <html lang='en'><head><link>some content<link></head>""");

        Stream.of(linkInternalizer1, linkInternalizer2)
                        .forEach(inliner -> verify(inliner, times(2)).inline(Map.of()));
    }

    @Test
    void shouldParseAttributesAndReplaceLinkTags() {
        when(linkInternalizer1.inline(anyMap())).thenReturn(Optional.of("<style>replacement</style>"));
        String resultHtml = htmlLinksHelper.internalizeLinks("""
                <html lang='en'><head><link attr1="value1" attr2="value2">some content</head>""");

        assertThat(resultHtml).isEqualTo("""
                <html lang='en'><head><style>replacement</style>some content</head>""");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(linkInternalizer1).inline(captor.capture());
        assertThat(captor.getValue()).containsExactly(Map.entry("attr1", "value1"), Map.entry("attr2", "value2"));
    }

    @Test
    void shouldReturnUnchangedHtmlWhenPropertyNotSet() {
        when(pdfExporterExtensionConfiguration.getInternalizeExternalCss()).thenReturn(false);
        String resultHtml = htmlLinksHelper.internalizeLinks("""
                    <html lang='en'><head><link attr1="value1" attr2="value2">some content</head>""");
        assertThat(resultHtml).isEqualTo("""
                <html lang='en'><head><link attr1="value1" attr2="value2">some content</head>""");
        verify(linkInternalizer1, times(0)).inline(anyMap());
    }
}