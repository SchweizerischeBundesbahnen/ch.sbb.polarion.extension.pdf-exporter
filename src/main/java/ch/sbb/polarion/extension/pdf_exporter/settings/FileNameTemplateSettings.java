package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import org.jetbrains.annotations.NotNull;

public class FileNameTemplateSettings extends GenericNamedSettings<FileNameTemplateModel> {
    public static final String FEATURE_NAME = "filename-template";
    public static final String DEFAULT_LIVE_DOC_NAME_TEMPLATE = "$projectName $document.moduleNameWithSpace.replace(\" / \", \" \")";
    public static final String DEFAULT_LIVE_REPORT_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") $page.lastRevision";
    public static final String DEFAULT_TEST_RUN_NAME_TEMPLATE = "$projectName $testrun.label";
    public static final String DEFAULT_WIKI_PAGE_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") $page.lastRevision";

    public FileNameTemplateSettings() {
        super(FEATURE_NAME);
    }

    public FileNameTemplateSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull FileNameTemplateModel defaultValues() {
        return FileNameTemplateModel.builder()
                .documentNameTemplate(DEFAULT_LIVE_DOC_NAME_TEMPLATE)
                .reportNameTemplate(DEFAULT_LIVE_REPORT_NAME_TEMPLATE)
                .testRunNameTemplate(DEFAULT_TEST_RUN_NAME_TEMPLATE)
                .wikiNameTemplate(DEFAULT_WIKI_PAGE_NAME_TEMPLATE)
                .build();
    }
}
