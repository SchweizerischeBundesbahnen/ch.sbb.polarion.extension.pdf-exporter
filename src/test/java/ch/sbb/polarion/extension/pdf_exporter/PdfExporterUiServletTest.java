package ch.sbb.polarion.extension.pdf_exporter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfExporterUiServletTest {

    @Test
    void testConstruction() {
        assertDoesNotThrow(PdfExporterUiServlet::new);
    }

}
