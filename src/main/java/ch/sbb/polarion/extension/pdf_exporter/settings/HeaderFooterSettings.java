package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class HeaderFooterSettings extends GenericNamedSettings<HeaderFooterModel> {

    public static final String FEATURE_NAME = "header-footer";

    public HeaderFooterSettings() {
        super(FEATURE_NAME);
    }

    public HeaderFooterSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull HeaderFooterModel defaultValues() {
        HeaderFooterModel model = HeaderFooterModel.builder()
                .headerLeft(wrapInPlaceholder(Placeholder.PROJECT_NAME.name()))
                .headerCenter("")
                .headerRight(
                        "<a href='https://www.sbb.ch/'>" + System.lineSeparator() +
                        "    <img src='/polarion/icons/group/sbb-headerlogo.png' " + System.lineSeparator() +
                        "         alt='Schweizerische Bundesbahnen' " + System.lineSeparator() +
                        "         style='height: 20px'>" + System.lineSeparator() +
                        "</a>")
                .footerLeft(wrapInPlaceholder(Placeholder.DOCUMENT_TITLE.name()) + " (rev. " + wrapInPlaceholder(Placeholder.REVISION.name()) + ")")
                .footerCenter(wrapInPlaceholder(Placeholder.TIMESTAMP.name()))
                .footerRight(wrapInPlaceholder(Placeholder.PAGE_NUMBER.name()) + "/" + wrapInPlaceholder(Placeholder.PAGES_TOTAL_COUNT.name()))
                .build();
        model.setDefaultHash(hash(model));
        return model;
    }

    /**
     * A new header and footer is empty and not in use, the built-in one applies until the user writes one.
     */
    public @NotNull HeaderFooterModel initialValues() {
        return HeaderFooterModel.builder()
                .headerLeft("")
                .headerCenter("")
                .headerRight("")
                .footerLeft("")
                .footerCenter("")
                .footerRight("")
                .build();
    }

    @Override
    public @NotNull HeaderFooterModel read(@NotNull String scope, @NotNull SettingId id, @Nullable String revisionName) {
        return withChangedDefault(withLegacyBase(withoutBuiltInCopy(super.read(scope, id, revisionName))));
    }

    /**
     * Reads a copy of the built-in header and footer nobody edited, which is not in use, as an empty one.
     */
    @VisibleForTesting
    @NotNull HeaderFooterModel withoutBuiltInCopy(@NotNull HeaderFooterModel model) {
        if (!model.isUseCustomValues() && isBuiltInCopy(model)) {
            HeaderFooterModel initial = initialValues();
            initial.setName(model.getName());
            initial.setBundleTimestamp(model.getBundleTimestamp());
            return initial;
        }
        return model;
    }

    /**
     * Gives a header and footer in use which is a copy of the built-in one, stored before copies remembered what they
     * copied, the hash of that version, so a newer built-in header and footer is noticed for it too.
     */
    @VisibleForTesting
    @NotNull HeaderFooterModel withLegacyBase(@NotNull HeaderFooterModel model) {
        if (model.isUseCustomValues() && model.getDefaultHash() == null && isBuiltInCopy(model)) {
            model.setDefaultHash(hash(model));
        }
        return model;
    }

    /**
     * Tells whether the built-in header and footer changed since the header and footer in use was copied from it.
     */
    @VisibleForTesting
    @NotNull HeaderFooterModel withChangedDefault(@NotNull HeaderFooterModel model) {
        model.setDefaultChanged(model.isUseCustomValues()
                && model.getDefaultHash() != null
                && !model.getDefaultHash().equals(defaultValues().getDefaultHash()));
        return model;
    }

    /**
     * @return whether a header and footer is a copy of the built-in one nobody edited: of the version it remembers, of
     * the current one or of one shipped before copies remembered their version
     */
    private boolean isBuiltInCopy(@NotNull HeaderFooterModel model) {
        String hash = hash(model);
        return hash.equals(model.getDefaultHash())
                || hash.equals(defaultValues().getDefaultHash())
                || BuiltInValues.isLegacy(FEATURE_NAME, hash);
    }

    private static @NotNull String hash(@NotNull HeaderFooterModel model) {
        return BuiltInValues.hash(model.getHeaderLeft(), model.getHeaderCenter(), model.getHeaderRight(),
                model.getFooterLeft(), model.getFooterCenter(), model.getFooterRight());
    }

    private String wrapInPlaceholder(String name) {
        return String.format("{{ %s }}", name);
    }
}
