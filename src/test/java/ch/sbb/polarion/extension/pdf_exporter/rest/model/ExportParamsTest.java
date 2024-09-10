package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import ch.sbb.polarion.extension.pdf_exporter.model.TranslationEntry;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportParamsTest {

    @Test
    void checkLocalizationDataParsedCorrectly() {

        Map<String, String> deTranslations = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4",
                "with space", "With Space"
        );
        LocalizationModel localizationModel = new LocalizationModel(deTranslations, null, null);
        final Map<String, List<TranslationEntry>> translations = localizationModel.getTranslations();

        assertEquals(5, translations.size());
        assertEquals(translations.get("key1"), List.of(new TranslationEntry(Locale.GERMAN.getLanguage(), "value1")));
        assertEquals(translations.get("key2"), List.of(new TranslationEntry(Locale.GERMAN.getLanguage(), "value2")));
        assertEquals(translations.get("key3"), List.of(new TranslationEntry(Locale.GERMAN.getLanguage(), "value3")));
        assertEquals(translations.get("with space"), List.of(new TranslationEntry(Locale.GERMAN.getLanguage(), "With Space")));
    }

}
