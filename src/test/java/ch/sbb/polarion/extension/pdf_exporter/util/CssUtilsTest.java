package ch.sbb.polarion.extension.pdf_exporter.util;

import com.steadystate.css.parser.CSSOMParser;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CssUtilsTest {

    @Test
    void dataUrlTest() {
        try (MockedConstruction<InputStreamReader> mockedReader = Mockito.mockConstruction(InputStreamReader.class,(mock, context) -> {
            throw new IOException();
        })) {
            assertDoesNotThrow(() -> CssUtils.parseCss(new CSSOMParser(), "some CSS"));
        }
    }
}
