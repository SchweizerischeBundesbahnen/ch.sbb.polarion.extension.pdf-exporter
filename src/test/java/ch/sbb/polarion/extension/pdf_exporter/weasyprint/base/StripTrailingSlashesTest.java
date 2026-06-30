package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripTrailingSlashesTest {

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:9080      | http://localhost:9080",
            "http://localhost:9080/     | http://localhost:9080",
            "http://localhost:9080///   | http://localhost:9080",
            "/                          | ''",
            "////                       | ''",
            "''                         | ''",
            "http://host/path/          | http://host/path",
    }, delimiterString = "|", ignoreLeadingAndTrailingWhitespace = true)
    void stripsTrailingSlashes(String input, String expected) {
        assertEquals(expected, BaseWeasyPrintTest.stripTrailingSlashes(input));
    }

    @Test
    void leavesInnerSlashesUntouched() {
        assertEquals("http://host//path", BaseWeasyPrintTest.stripTrailingSlashes("http://host//path//"));
    }

    @Test
    void isLinearOnManyTrailingSlashes() {
        String input = "x" + "/".repeat(100_000);
        assertEquals("x", BaseWeasyPrintTest.stripTrailingSlashes(input));
    }
}
