package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.css;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CssModelTest {

    private static Stream<Arguments> testValuesForCss() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of("", null, null),
                Arguments.of("some badly formatted string", null, null),
                Arguments.of(String.format("ok file" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN CSS-----%1$s" +
                                "css content" +
                                "-----END CSS-----%1$s",
                        System.lineSeparator()), "12345", "css content"),
                Arguments.of(String.format("no bundle timestamp" +
                                "-----BEGIN CSS-----%1$s" +
                                "css content" +
                                "-----END CSS-----%1$s",
                        System.lineSeparator()), null, "css content"),
                Arguments.of(String.format("keep first duplicated entry" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "ts1%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "ts2%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN CSS-----%1$s" +
                                "css content 1" +
                                "-----END CSS-----%1$s" +
                                "-----BEGIN CSS-----%1$s" +
                                "css content 2" +
                                "-----END CSS-----%1$s",
                        System.lineSeparator()), "ts1", "css content 1")
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForCss")
    void getProperExpectedResults(String locationContent, String expectedBundleTimestamp, String expectedCss) {

        final CssModel model = new CssModel();
        model.deserialize(locationContent);

        assertEquals(expectedBundleTimestamp, model.getBundleTimestamp());
        assertEquals(expectedCss, model.getCss());
    }
}