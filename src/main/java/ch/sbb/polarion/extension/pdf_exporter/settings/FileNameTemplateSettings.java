package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class FileNameTemplateSettings extends GenericNamedSettings<FileNameTemplateModel> {
    public static final String FEATURE_NAME = "filename-template";
    public static final String DEFAULT_LIVE_DOC_NAME_TEMPLATE = "$projectName $document.moduleNameWithSpace.replace(\" / \", \" \")";
    public static final String DEFAULT_LIVE_REPORT_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") {{ REVISION }}";
    public static final String DEFAULT_TEST_RUN_NAME_TEMPLATE = "$projectName $testrun.label";
    public static final String DEFAULT_WIKI_PAGE_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") {{ REVISION }}";

    public FileNameTemplateSettings() {
        super(FEATURE_NAME);
    }

    public FileNameTemplateSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull FileNameTemplateModel defaultValues() {
        FileNameTemplateModel model = FileNameTemplateModel.builder()
                .documentNameTemplate(DEFAULT_LIVE_DOC_NAME_TEMPLATE)
                .reportNameTemplate(DEFAULT_LIVE_REPORT_NAME_TEMPLATE)
                .testRunNameTemplate(DEFAULT_TEST_RUN_NAME_TEMPLATE)
                .wikiNameTemplate(DEFAULT_WIKI_PAGE_NAME_TEMPLATE)
                .build();
        model.setDefaultHash(hash(model));
        return model;
    }

    /**
     * New filename templates are empty and not in use, the built-in ones apply until the user writes some.
     */
    public @NotNull FileNameTemplateModel initialValues() {
        return FileNameTemplateModel.builder()
                .documentNameTemplate("")
                .reportNameTemplate("")
                .testRunNameTemplate("")
                .wikiNameTemplate("")
                .build();
    }

    @Override
    public @NotNull FileNameTemplateModel read(@NotNull String scope, @NotNull SettingId id, @Nullable String revisionName) {
        return withChangedDefault(withLegacyBase(withoutBuiltInCopy(super.read(scope, id, revisionName))));
    }

    /**
     * Reads a copy of the built-in filename templates nobody edited, which is not in use, as empty templates.
     */
    @VisibleForTesting
    @NotNull FileNameTemplateModel withoutBuiltInCopy(@NotNull FileNameTemplateModel model) {
        if (!model.isUseCustomValues() && isBuiltInCopy(model)) {
            FileNameTemplateModel initial = initialValues();
            initial.setName(model.getName());
            initial.setBundleTimestamp(model.getBundleTimestamp());
            return initial;
        }
        return model;
    }

    /**
     * Gives filename templates in use which are a copy of the built-in ones, stored before copies remembered what they
     * copied, the hash of that version, so newer built-in templates are noticed for them too.
     */
    @VisibleForTesting
    @NotNull FileNameTemplateModel withLegacyBase(@NotNull FileNameTemplateModel model) {
        if (model.isUseCustomValues() && model.getDefaultHash() == null && isBuiltInCopy(model)) {
            model.setDefaultHash(hash(model));
        }
        return model;
    }

    /**
     * Tells whether the built-in filename templates changed since the templates in use were copied from them.
     */
    @VisibleForTesting
    @NotNull FileNameTemplateModel withChangedDefault(@NotNull FileNameTemplateModel model) {
        model.setDefaultChanged(model.isUseCustomValues()
                && model.getDefaultHash() != null
                && !model.getDefaultHash().equals(defaultValues().getDefaultHash()));
        return model;
    }

    /**
     * @return whether filename templates are a copy of the built-in ones nobody edited: of the version they remember, of
     * the current one or of one shipped before copies remembered their version
     */
    private boolean isBuiltInCopy(@NotNull FileNameTemplateModel model) {
        String hash = hash(model);
        return hash.equals(model.getDefaultHash())
                || hash.equals(defaultValues().getDefaultHash())
                || BuiltInValues.isLegacy(FEATURE_NAME, hash);
    }

    private static @NotNull String hash(@NotNull FileNameTemplateModel model) {
        return BuiltInValues.hash(model.getDocumentNameTemplate(), model.getReportNameTemplate(),
                model.getTestRunNameTemplate(), model.getWikiNameTemplate());
    }
}
