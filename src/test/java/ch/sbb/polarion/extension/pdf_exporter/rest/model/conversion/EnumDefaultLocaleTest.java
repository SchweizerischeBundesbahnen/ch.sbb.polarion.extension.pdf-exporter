package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Turkish locale maps "i" and "I" to letters which no constant name contains.
 */
class EnumDefaultLocaleTest {

    private Locale defaultLocale;

    @BeforeEach
    void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    @ParameterizedTest
    @EnumSource
    void parsesPaperSize(PaperSize paperSize) {
        assertEquals(paperSize, PaperSize.fromString(lowerCased(paperSize)));
    }

    @ParameterizedTest
    @EnumSource
    void parsesOrientation(Orientation orientation) {
        assertEquals(orientation, Orientation.fromString(lowerCased(orientation)));
    }

    @ParameterizedTest
    @EnumSource
    void parsesPdfVariant(PdfVariant pdfVariant) {
        assertEquals(pdfVariant, PdfVariant.fromString(lowerCased(pdfVariant)));
    }

    @ParameterizedTest
    @EnumSource
    void parsesDocumentType(DocumentType documentType) {
        assertEquals(documentType, DocumentType.fromString(lowerCased(documentType)));
    }

    @ParameterizedTest
    @EnumSource
    void parsesLanguage(Language language) {
        assertEquals(language, Language.valueOfIgnoreCase(lowerCased(language)));
    }

    @Test
    void writesOrientationAsCss() {
        assertEquals("portrait", Orientation.PORTRAIT.toCssString());
        assertEquals("landscape", Orientation.LANDSCAPE.toCssString());
    }

    @Test
    void writesPaperSizeAsCss() {
        assertEquals("letter", PaperSize.LETTER.toCssString());
        assertEquals("legal", PaperSize.LEGAL.toCssString());
        assertEquals("ledger", PaperSize.LEDGER.toCssString());
    }

    private static String lowerCased(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT);
    }
}
