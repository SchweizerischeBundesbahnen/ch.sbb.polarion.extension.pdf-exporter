package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageNamedByTest {

    private static final String PROPERTY = "pdf-exporter.test.image";
    private static final String DEFAULT_IMAGE = "registry/service:latest";

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void takesTheDefaultWhereThePropertyIsNotSet() {
        assertEquals(DEFAULT_IMAGE, SharedWeasyPrintContainer.imageNamedBy(PROPERTY, DEFAULT_IMAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void takesTheDefaultWhereThePropertyIsBlank(String value) {
        System.setProperty(PROPERTY, value);

        assertEquals(DEFAULT_IMAGE, SharedWeasyPrintContainer.imageNamedBy(PROPERTY, DEFAULT_IMAGE));
    }

    @Test
    void takesTheImageThePropertyNames() {
        System.setProperty(PROPERTY, " weasyprint-service:candidate ");

        assertEquals("weasyprint-service:candidate", SharedWeasyPrintContainer.imageNamedBy(PROPERTY, DEFAULT_IMAGE));
    }
}
