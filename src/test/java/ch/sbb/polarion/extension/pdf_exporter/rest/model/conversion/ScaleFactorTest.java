package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

class ScaleFactorTest {
    @Test
    void testFromStringValidValues() {
        for (ScaleFactor factor : ScaleFactor.values()) {
            String name = factor.name().toLowerCase();
            ScaleFactor result = ScaleFactor.fromString(name);
            assertEquals(factor, result, "Should parse " + name + " correctly");
        }
    }

    @Test
    void testFromStringNull() {
        assertNull(ScaleFactor.fromString(null), "Should return null for null input");
    }

    @Test
    void testFromStringInvalidValue() {
        String invalid = "dpi_999";
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> ScaleFactor.fromString(invalid));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getResponse().getEntity().toString().contains(invalid));
    }

    @Test
    void testScaleValues() {
        assertEquals(1.0, ScaleFactor.DPI_96.getScale());
        assertEquals(2.0, ScaleFactor.DPI_192.getScale());
        assertEquals(3.125, ScaleFactor.DPI_300.getScale());
        assertEquals(6.25, ScaleFactor.DPI_600.getScale());
    }
}
