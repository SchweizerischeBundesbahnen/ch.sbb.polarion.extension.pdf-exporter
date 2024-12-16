package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WildcardUtilsTest {

    static Stream<Arguments> provideToRegexArguments() {
        return Stream.of(
                Arguments.of(null, ".*"),
                Arguments.of("", ".*"),
                Arguments.of("*.txt", "^.*\\.txt$"),
                Arguments.of("file?.doc", "^file.\\.doc$"),
                Arguments.of("doc*", "^doc.*$"),
                Arguments.of("*.*.*", "^.*\\..*\\..*$"),
                Arguments.of("plainText", "^plainText$")
        );
    }

    @ParameterizedTest(name = "Wildcard: {0} -> Regex: {1}")
    @MethodSource("provideToRegexArguments")
    void testToRegex(String wildcard, String expectedRegex) {
        String actualRegex = WildcardUtils.toRegex(wildcard);
        assertEquals(expectedRegex, actualRegex);
    }

    static Stream<Arguments> provideMatchesArguments() {
        return Stream.of(
                Arguments.of(null, "*.txt", false),
                Arguments.of("", "*.txt", false),
                Arguments.of("file.txt", "*.txt", true),
                Arguments.of("file.doc", "*.txt", false),
                Arguments.of("doc1.doc", "doc?.doc", true),
                Arguments.of("document.doc", "doc?.doc", false),
                Arguments.of("config.yaml", "config.*", true),
                Arguments.of("log.2024-11-27.txt", "log.????-??-??.txt", true),
                Arguments.of("log.24-11-27.txt", "log.????-??-??.txt", false)
        );
    }

    @ParameterizedTest(name = "Text: {0}, Wildcard: {1}, Matches: {2}")
    @MethodSource("provideMatchesArguments")
    void testMatches(String text, String wildcard, boolean expectedResult) {
        boolean actualResult = WildcardUtils.matches(text, wildcard);
        assertEquals(expectedResult, actualResult);
    }
}
