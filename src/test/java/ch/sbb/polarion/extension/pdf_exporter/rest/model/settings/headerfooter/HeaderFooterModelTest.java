package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderFooterModelTest {

    private static Stream<Arguments> testValuesForGetHeaderFooter() {
        return Stream.of(
                Arguments.of(null, null, null, null, null, null, null, null),
                Arguments.of("", null, null, null, null, null, null, null),
                Arguments.of("some badly formatted string", null, null, null, null, null, null, null),
                Arguments.of(String.format("ok file" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN HEADER LEFT-----%1$s" +
                                "hl%1$s" +
                                "-----END HEADER LEFT-----%1$s" +
                                "-----BEGIN HEADER CENTER-----%1$s" +
                                "hc%1$s" +
                                "-----END HEADER CENTER-----%1$s" +
                                "-----BEGIN HEADER RIGHT-----%1$s" +
                                "hr%1$s" +
                                "-----END HEADER RIGHT-----%1$s" +
                                "-----BEGIN FOOTER LEFT-----%1$s" +
                                "fl%1$s" +
                                "-----END FOOTER LEFT-----%1$s" +
                                "-----BEGIN FOOTER CENTER-----%1$s" +
                                "fc%1$s" +
                                "-----END FOOTER CENTER-----%1$s" +
                                "-----BEGIN FOOTER RIGHT-----%1$s" +
                                "fr%1$s" +
                                "-----END FOOTER RIGHT-----%1$s",
                        System.lineSeparator()), "12345", "hl", "hc", "hr", "fl", "fc", "fr"),
                Arguments.of(String.format("ok file mixed order" +
                                "-----BEGIN FOOTER CENTER-----%1$s" +
                                "fc%1$s" +
                                "-----END FOOTER CENTER-----%1$s" +
                                "-----BEGIN BUNDLE TIMESTAMP-----%1$s" +
                                "12345%1$s" +
                                "-----END BUNDLE TIMESTAMP-----%1$s" +
                                "-----BEGIN HEADER CENTER-----%1$s" +
                                "hc%1$s" +
                                "-----END HEADER CENTER-----%1$s" +
                                "-----BEGIN HEADER RIGHT-----%1$s" +
                                "hr%1$s" +
                                "-----END HEADER RIGHT-----%1$s" +
                                "-----BEGIN FOOTER LEFT-----%1$s" +
                                "fl%1$s" +
                                "-----END FOOTER LEFT-----%1$s" +
                                "-----BEGIN HEADER LEFT-----%1$s" +
                                "hl%1$s" +
                                "-----END HEADER LEFT-----%1$s" +
                                "-----BEGIN FOOTER RIGHT-----%1$s" +
                                "fr%1$s" +
                                "-----END FOOTER RIGHT-----%1$s",
                        System.lineSeparator()), "12345", "hl", "hc", "hr", "fl", "fc", "fr"),
                Arguments.of(String.format("missing parts" +
                                "-----BEGIN HEADER CENTER-----%1$s" +
                                "hc%1$s" +
                                "-----END HEADER CENTER-----%1$s" +
                                "-----BEGIN FOOTER LEFT-----%1$s" +
                                "fl%1$s" +
                                "-----END FOOTER LEFT-----%1$s",
                        System.lineSeparator()), null, null, "hc", null, "fl", null, null),
                Arguments.of(String.format("cut only starting and ending separators" +
                                "-----BEGIN HEADER CENTER-----%1$s" +
                                "%1$s" +
                                "%1$s-----END HEADER CENTER-----%1$s",
                        System.lineSeparator()), null, null, System.lineSeparator(), null, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("testValuesForGetHeaderFooter")
    void getProperExpectedResults(String locationContent, String expectedBundleTimestamp,
                                  String expectedHeaderLeft, String expectedHeaderCenter, String expectedHeaderRight,
                                  String expectedFooterLeft, String expectedFooterCenter, String expectedFooterRight) {

        final HeaderFooterModel model = new HeaderFooterModel();
        model.deserialize(locationContent);

        assertEquals(expectedBundleTimestamp, model.getBundleTimestamp());

        assertEquals(expectedHeaderLeft, model.getHeaderLeft());
        assertEquals(expectedHeaderCenter, model.getHeaderCenter());
        assertEquals(expectedHeaderRight, model.getHeaderRight());
        assertEquals(expectedFooterLeft, model.getFooterLeft());
        assertEquals(expectedFooterCenter, model.getFooterCenter());
        assertEquals(expectedFooterRight, model.getFooterRight());
    }
}