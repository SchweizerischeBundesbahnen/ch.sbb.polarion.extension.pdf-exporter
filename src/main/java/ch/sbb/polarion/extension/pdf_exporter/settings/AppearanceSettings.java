package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.appearance.AppearanceModel;
import org.jetbrains.annotations.NotNull;

/**
 * Where the extension's toolbar buttons are shown. One setting named {@code Default} per scope: a
 * project without one inherits the Default Repository setting, which {@code GenericNamedSettings.read}
 * falls back to. So an administrator configures every project at once in the Default Repository, and a
 * project overrides it by saving its own.
 */
public class AppearanceSettings extends GenericNamedSettings<AppearanceModel> {
    public static final String FEATURE_NAME = "appearance";

    public AppearanceSettings() {
        super(FEATURE_NAME);
    }

    public AppearanceSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull AppearanceModel defaultValues() {
        return AppearanceModel.builder()
                .liveDocButton(AppearanceModel.DEFAULT_DISPLAY)
                .liveReportButton(AppearanceModel.DEFAULT_DISPLAY)
                .liveReportAutoExpandTools(AppearanceModel.DEFAULT_AUTO_EXPAND_TOOLS)
                .classicWikiButton(AppearanceModel.DEFAULT_DISPLAY)
                .classicWikiAutoExpandTools(AppearanceModel.DEFAULT_AUTO_EXPAND_TOOLS)
                .build();
    }
}
