package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Integration test for the document-language feature (#983): the language a document states in its custom field is
 * injected into the {@code <html lang>} attribute and drives WeasyPrint's hyphenation.
 * <p>
 * Each fixture contains long words in a narrow, justified column, and the custom CSS asks for {@code hyphens: auto}; the
 * {@code hyphenationDe}/{@code hyphenationEn} fixtures additionally exercise narrow table cells. WeasyPrint
 * hyphenates at the syllable boundaries of the document's language, so the rendered pages must match
 * language-specific reference images. This proves the language field actually reaches WeasyPrint and is honored per
 * language.
 */
class HyphenationTest extends BasePdfConverterTest {

    private static final String LANGUAGE_FIELD = "docLanguage";

    /** The default CSS, with the hyphenation and the narrow columns a user adds to it. */
    @Override
    protected void setupCssSettings() {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(readFontCss() + readCss("hyphenation"))
                .build());
    }

    @Test
    void germanHyphenationMatchesReference() {
        assertMatchesReference("hyphenationDe", "de");
    }

    @Test
    void englishHyphenationMatchesReference() {
        assertMatchesReference("hyphenationEn", "en");
    }

    @Test
    void germanLongCompoundHyphenationMatchesReference() {
        assertMatchesReference("hyphenationDeLongWords", "de");
    }

    @Test
    void italianHyphenationMatchesReference() {
        assertMatchesReference("hyphenationIt", "it");
    }

    private void assertMatchesReference(String testName, String language) {
        lenient().when(module.getCustomField(LANGUAGE_FIELD)).thenReturn(language);
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .languageCustomField(LANGUAGE_FIELD)
                .build();

        byte[] pdf = exportLiveDoc("Hyphenation", readHtmlResource(testName), params);

        assertEquals(1, pageCount(pdf), "Hyphenation export should produce a single page");
        assertFalse(compareContentUsingReferenceImages(testName, pdf), "Generated PDF should match reference image (differences highlighted in blue in reports folder)");
    }

    @SneakyThrows
    private static @NotNull String readCss(@NotNull String name) {
        try (InputStream css = HyphenationTest.class.getResourceAsStream(WEASYPRINT_TEST_CSS_RESOURCES_FOLDER + name + EXT_CSS)) {
            return new String(Objects.requireNonNull(css, name).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SneakyThrows
    private static int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
