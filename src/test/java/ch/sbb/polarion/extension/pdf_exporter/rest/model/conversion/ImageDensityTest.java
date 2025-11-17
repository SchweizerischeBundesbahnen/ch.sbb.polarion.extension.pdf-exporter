package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

class ImageDensityTest {
    @Test
    void testFromStringValidValues() {
        for (ImageDensity factor : ImageDensity.values()) {
            String name = factor.name().toLowerCase();
            ImageDensity result = ImageDensity.fromString(name);
            assertEquals(factor, result, "Should parse " + name + " correctly");
        }
    }

    @Test
    void testFromStringNull() {
        assertNull(ImageDensity.fromString(null), "Should return null for null input");
    }

    @Test
    void testFromStringInvalidValue() {
        String invalid = "dpi_999";
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> ImageDensity.fromString(invalid));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getResponse().getEntity().toString().contains(invalid));
    }

    @Test
    void testScaleValues() {
        assertEquals(1.0, ImageDensity.DPI_96.getScale());
        assertEquals(2.0, ImageDensity.DPI_192.getScale());
        assertEquals(3.125, ImageDensity.DPI_300.getScale());
        assertEquals(6.25, ImageDensity.DPI_600.getScale());
    }
}
