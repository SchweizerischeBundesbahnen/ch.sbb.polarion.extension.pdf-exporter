package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.TestStringUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NumberedListsSanitizerTest {

    private NumberedListsSanitizer sanitizer;

    @BeforeEach
    void init() {
        sanitizer = new NumberedListsSanitizer();
    }

    @Test
    @SneakyThrows
    void fixNumberedListsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/invalidNumberedLists.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/validNumberedLists.html")) {

            // Spaces and new lines are removed to exclude difference in space characters
            assertNotNull(isInvalidHtml);
            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);
            String fixedHtml = sanitizer.fixNumberedLists(invalidHtml).replaceAll(" ", "");
            assertNotNull(isValidHtml);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @ParameterizedTest
    @MethodSource("getNestedNumberedListsParameters")
    void htmlContainsNestedNumberedLists(String html, boolean expectedResult) {
        boolean result = sanitizer.containsNestedNumberedLists(html);
        assertEquals(result, expectedResult);
    }

    private static Stream<Arguments> getNestedNumberedListsParameters() {
        return Stream.of(
                Arguments.of("""
                        <ol>
                            <li>sub-sub-sub-item3</li>
                            <li>sub-sub-sub-item4
                                <ol>
                                    <li>sub-sub-sub-sub-item1</li>
                                    <li>sub-sub-sub-sub-item2</li>
                                </ol>
                            </li>
                        </ol>""", true),
                Arguments.of("""
                        <ol>
                            <li>sub-sub-sub-item3</li>
                            <li>sub-sub-sub-item4
                            </li>
                        </ol>""", false));
    }

}
