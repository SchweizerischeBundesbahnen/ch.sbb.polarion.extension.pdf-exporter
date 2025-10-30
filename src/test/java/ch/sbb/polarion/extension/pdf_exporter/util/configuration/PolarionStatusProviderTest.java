package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PolarionStatusProviderTest {

    @Test
    void testConstruction() {
        assertDoesNotThrow(PolarionStatusProvider::new);
    }

}
