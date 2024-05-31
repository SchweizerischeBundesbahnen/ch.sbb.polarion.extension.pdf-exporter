package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.css.CssModel;
import org.jetbrains.annotations.NotNull;

public class CssSettings extends GenericNamedSettings<CssModel> {
    public static final String FEATURE_NAME = "css";

    public CssSettings() {
        super(FEATURE_NAME);
    }

    public CssSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull CssModel defaultValues() {
        return CssModel.builder().css(ScopeUtils.getFileContent("default/dle-pdf-export.css")).build();
    }

}
