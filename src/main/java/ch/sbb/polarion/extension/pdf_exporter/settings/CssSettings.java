package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
        String css = ScopeUtils.getFileContent("default/dle-pdf-export.css");
        return CssModel.builder().css(css).defaultHash(BuiltInValues.hash(css)).build();
    }

    /**
     * A new CSS setting has no custom CSS, so the export applies the default CSS alone.
     */
    public @NotNull CssModel initialValues() {
        return CssModel.builder().build();
    }

    @Override
    public @NotNull CssModel read(@NotNull String scope, @NotNull SettingId id, @Nullable String revisionName) {
        return withChangedDefault(withLegacyBase(withoutBuiltInCopy(super.read(scope, id, revisionName))));
    }

    /**
     * Reads a copy of the default CSS nobody edited as no custom CSS while the default CSS is enabled: the export would
     * apply the default CSS twice. With the default CSS disabled the copy is the only CSS, so it stays.
     */
    @VisibleForTesting
    @NotNull CssModel withoutBuiltInCopy(@NotNull CssModel model) {
        if (!model.isDisableDefaultCss() && isBuiltInCopy(model)) {
            model.setCss("");
            model.setDefaultHash(null);
        }
        return model;
    }

    /**
     * Gives a copy of the default CSS stored before copies remembered what they copied the hash of that version, so a
     * newer default CSS is noticed for it too.
     */
    @VisibleForTesting
    @NotNull CssModel withLegacyBase(@NotNull CssModel model) {
        if (model.isDisableDefaultCss() && model.getDefaultHash() == null && isBuiltInCopy(model)) {
            model.setDefaultHash(BuiltInValues.hash(model.getCss()));
        }
        return model;
    }

    /**
     * Tells whether the default CSS changed since the custom CSS was copied from it. Only the custom CSS alone can fall
     * behind: with both, the export applies the current default CSS.
     */
    @VisibleForTesting
    @NotNull CssModel withChangedDefault(@NotNull CssModel model) {
        model.setDefaultChanged(model.isDisableDefaultCss()
                && model.getDefaultHash() != null
                && !model.getDefaultHash().equals(defaultValues().getDefaultHash()));
        return model;
    }

    /**
     * @return whether the custom CSS is a copy of the default CSS nobody edited: of the version it remembers, of the
     * current one or of one shipped before copies remembered their version
     */
    private boolean isBuiltInCopy(@NotNull CssModel model) {
        String hash = BuiltInValues.hash(model.getCss());
        return hash.equals(model.getDefaultHash())
                || hash.equals(defaultValues().getDefaultHash())
                || BuiltInValues.isLegacy(FEATURE_NAME, hash);
    }

}
