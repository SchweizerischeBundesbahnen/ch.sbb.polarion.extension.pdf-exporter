package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf_exporter.util.LocalizationHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LocalizationSettings extends GenericNamedSettings<LocalizationModel> {

    public static final String FEATURE_NAME = "localization";

    public LocalizationSettings() {
        super(FEATURE_NAME);
    }

    public LocalizationSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull LocalizationModel defaultValues() {
        Map<String, String> deTranslations = LocalizationHelper.getTranslationsMapForLanguage(defaultLocalization("default/de.xlf"));
        Map<String, String> frTranslations = LocalizationHelper.getTranslationsMapForLanguage(defaultLocalization("default/fr.xlf"));
        Map<String, String> itTranslations = LocalizationHelper.getTranslationsMapForLanguage(defaultLocalization("default/it.xlf"));

        return new LocalizationModel(deTranslations, frTranslations, itTranslations);
    }

    private @NotNull String defaultLocalization(@NotNull String filename) {
        return ScopeUtils.getFileContent(filename);
    }
}
