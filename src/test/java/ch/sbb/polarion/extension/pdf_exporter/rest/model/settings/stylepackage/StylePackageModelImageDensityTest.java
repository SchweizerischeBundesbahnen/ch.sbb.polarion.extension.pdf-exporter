package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StylePackageModelImageDensityTest {

    /** A style package stored before the image density was dropped still carries it. */
    private static final String IMAGE_DENSITY_ENTRY = System.lineSeparator() +
            "-----BEGIN IMAGE DENSITY-----" + System.lineSeparator() +
            "DPI_300" + System.lineSeparator() +
            "-----END IMAGE DENSITY-----" + System.lineSeparator();

    @Test
    void readsAStylePackageStoredWithAnImageDensity() {
        StylePackageModel model = StylePackageModel.builder().paperSize("A3").pdfVariant("PDF_UA_1").fullFonts(true).build();

        StylePackageModel read = new StylePackageModel();
        read.deserialize(model.serialize() + IMAGE_DENSITY_ENTRY);

        assertEquals("A3", read.getPaperSize());
        assertEquals("PDF_UA_1", read.getPdfVariant());
        assertTrue(read.isFullFonts());
    }

    @Test
    void storesNoImageDensity() {
        StylePackageModel read = new StylePackageModel();
        read.deserialize(StylePackageModel.builder().paperSize("A3").build().serialize() + IMAGE_DENSITY_ENTRY);

        assertFalse(read.serialize().contains("IMAGE DENSITY"));
    }
}
