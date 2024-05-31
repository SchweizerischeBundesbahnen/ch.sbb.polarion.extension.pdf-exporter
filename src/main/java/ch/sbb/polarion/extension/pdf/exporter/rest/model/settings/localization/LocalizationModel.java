package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.localization;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import ch.sbb.polarion.extension.pdf.exporter.model.TranslationEntry;
import ch.sbb.polarion.extension.pdf.exporter.util.LocalizationHelper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.alm.tracker.importer.docanalysis.rules.InvalidArgumentException;
import com.polarion.core.util.logging.Logger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("java:S1166")
public class LocalizationModel extends SettingsModel {

    private static final Logger logger = Logger.getLogger(LocalizationModel.class);

    public static final String DE_LOCALIZATION = "DE LOCALIZATION";
    public static final String FR_LOCALIZATION = "FR LOCALIZATION";
    public static final String IT_LOCALIZATION = "IT LOCALIZATION";

    private Map<String, List<TranslationEntry>> translations;

    public LocalizationModel(Map<String, String> deTranslations, Map<String, String> frTranslations, Map<String, String> itTranslations) {
        createTranslationsMap(deTranslations, frTranslations, itTranslations);
    }

    @Override
    protected String serializeModelData() {
        String de = LocalizationHelper.xmlForLanguage(Locale.GERMAN.getLanguage(), translations);
        String fr = LocalizationHelper.xmlForLanguage(Locale.FRENCH.getLanguage(), translations);
        String it = LocalizationHelper.xmlForLanguage(Locale.ITALIAN.getLanguage(), translations);

        return serializeEntry(DE_LOCALIZATION, de) +
                serializeEntry(FR_LOCALIZATION, fr) +
                serializeEntry(IT_LOCALIZATION, it);
    }

    @Override
    @SuppressWarnings("java:S1166")
    protected void deserializeModelData(String serializedString) {
        try {
            String de = deserializeEntry(DE_LOCALIZATION, serializedString);
            String fr = deserializeEntry(FR_LOCALIZATION, serializedString);
            String it = deserializeEntry(IT_LOCALIZATION, serializedString);
            Map<String, String> deTranslations = LocalizationHelper.getTranslationsMapForLanguage(de);
            Map<String, String> frTranslations = LocalizationHelper.getTranslationsMapForLanguage(fr);
            Map<String, String> itTranslations = LocalizationHelper.getTranslationsMapForLanguage(it);
            createTranslationsMap(deTranslations, frTranslations, itTranslations);
        } catch (Exception e) {
            throw new InvalidArgumentException("Error parsing translations, please try to load and save default values");
        }
    }

    private void createTranslationsMap(Map<String, String> deTranslations, Map<String, String> frTranslations, Map<String, String> itTranslations) {
        translations = new TreeMap<>();
        addTranslationsByLanguage(Locale.GERMAN.getLanguage(), deTranslations);
        addTranslationsByLanguage(Locale.FRENCH.getLanguage(), frTranslations);
        addTranslationsByLanguage(Locale.ITALIAN.getLanguage(), itTranslations);
    }

    private void addTranslationsByLanguage(String language, Map<String, String> source) {
        if (source != null) {
            source.keySet().forEach(translationKey -> {
                String value = source.get(translationKey);
                TranslationEntry translationEntry = new TranslationEntry(language, value);
                translations.computeIfAbsent(translationKey, val -> new ArrayList<>())
                        .add(translationEntry);
            });
        }
    }

    public Map<String, String> getLocalizationMap(String lang) {
        Language language = Language.valueOfIgnoreCase(lang);

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, List<TranslationEntry>> entry : translations.entrySet()) {
            String key = entry.getKey();
            List<TranslationEntry> translationEntries = entry.getValue();

            if (!translationEntries.isEmpty()) {
                for (TranslationEntry translationEntry : translationEntries) {
                    if (translationEntry.getLanguage().equalsIgnoreCase(language.name())) {
                        result.put(key, translationEntry.getValue());
                    }
                }
            }
        }

        return result;
    }
}
