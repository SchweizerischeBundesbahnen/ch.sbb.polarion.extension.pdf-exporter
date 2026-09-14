package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class BuiltInValuesTest {

    @Test
    void testHashIgnoresWhitespaceAroundAndLineEndings() {
        assertEquals(BuiltInValues.hash("a\nb", "c"), BuiltInValues.hash("  a\r\nb\n", "c "));
        assertEquals(BuiltInValues.hash("", ""), BuiltInValues.hash(null, null));
        assertNotEquals(BuiltInValues.hash("a", "b"), BuiltInValues.hash("ab", ""));
        assertNotEquals(BuiltInValues.hash("a", "b"), BuiltInValues.hash("b", "a"));
    }

    @Test
    void testUnknownHash() {
        assertFalse(BuiltInValues.isLegacy(CssSettings.FEATURE_NAME, null));
        assertFalse(BuiltInValues.isLegacy(CssSettings.FEATURE_NAME, BuiltInValues.hash("custom")));
        assertFalse(BuiltInValues.isLegacy("unknown-feature", new CssSettings(new SettingsService(null, null, null)).defaultValues().getDefaultHash()));
    }

    @ParameterizedTest
    @ValueSource(strings = {CssSettings.FEATURE_NAME, CoverPageSettings.FEATURE_NAME, HeaderFooterSettings.FEATURE_NAME, FileNameTemplateSettings.FEATURE_NAME})
    void testLegacyValuesListEveryFeature(String feature) throws IOException {
        assertFalse(history().getOrDefault(feature, List.of()).isEmpty());
    }

    /**
     * @return a hash of built-in values a former version of a feature shipped
     */
    static String formerHash(String feature, Set<String> currentHashes) throws IOException {
        return history().get(feature).stream().filter(hash -> !currentHashes.contains(hash)).findFirst().orElseThrow();
    }

    private static Map<String, List<String>> history() throws IOException {
        try (InputStream stream = BuiltInValuesTest.class.getClassLoader().getResourceAsStream(BuiltInValues.LEGACY_RESOURCE)) {
            return new ObjectMapper().readValue(stream, new TypeReference<>() {
            });
        }
    }
}
