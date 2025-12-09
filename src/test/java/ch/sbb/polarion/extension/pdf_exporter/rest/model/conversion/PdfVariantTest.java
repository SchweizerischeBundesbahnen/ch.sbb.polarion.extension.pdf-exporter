package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

class PdfVariantTest {

    @Test
    void testFromStringValidValues() {
        for (PdfVariant variant : PdfVariant.values()) {
            String name = variant.name().toLowerCase();
            PdfVariant result = PdfVariant.fromString(name);
            assertEquals(variant, result, "Should parse " + name + " correctly");
        }
    }

    @Test
    void testFromStringNull() {
        assertNull(PdfVariant.fromString(null), "Should return null for null input");
    }

    @Test
    void testFromStringInvalidValue() {
        String invalid = "pdf_a_999";
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> PdfVariant.fromString(invalid));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getResponse().getEntity().toString().contains(invalid));
    }

    @Test
    void testToWeasyPrintParameter() {
        assertEquals("pdf/a-1a", PdfVariant.PDF_A_1A.toWeasyPrintParameter());
        assertEquals("pdf/a-1b", PdfVariant.PDF_A_1B.toWeasyPrintParameter());
        assertEquals("pdf/a-2a", PdfVariant.PDF_A_2A.toWeasyPrintParameter());
        assertEquals("pdf/a-2b", PdfVariant.PDF_A_2B.toWeasyPrintParameter());
        assertEquals("pdf/a-2u", PdfVariant.PDF_A_2U.toWeasyPrintParameter());
        assertEquals("pdf/a-3a", PdfVariant.PDF_A_3A.toWeasyPrintParameter());
        assertEquals("pdf/a-3b", PdfVariant.PDF_A_3B.toWeasyPrintParameter());
        assertEquals("pdf/a-3u", PdfVariant.PDF_A_3U.toWeasyPrintParameter());
        assertEquals("pdf/a-4e", PdfVariant.PDF_A_4E.toWeasyPrintParameter());
        assertEquals("pdf/a-4f", PdfVariant.PDF_A_4F.toWeasyPrintParameter());
        assertEquals("pdf/a-4u", PdfVariant.PDF_A_4U.toWeasyPrintParameter());
        assertEquals("pdf/ua-1", PdfVariant.PDF_UA_1.toWeasyPrintParameter());
        assertEquals("pdf/ua-2", PdfVariant.PDF_UA_2.toWeasyPrintParameter());
    }

}
