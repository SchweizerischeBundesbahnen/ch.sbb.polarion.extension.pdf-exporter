package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.hooks.HooksModel;
import org.jetbrains.annotations.NotNull;

public class HooksSettings extends GenericNamedSettings<HooksModel> {
    public static final String FEATURE_NAME = "hooks";

    public HooksSettings() {
        super(FEATURE_NAME);
    }

    public HooksSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull HooksModel defaultValues() {
        return HooksModel.builder().build();
    }
}
