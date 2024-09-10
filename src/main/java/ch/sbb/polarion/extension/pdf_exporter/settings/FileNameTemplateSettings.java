package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import org.jetbrains.annotations.NotNull;

public class FileNameTemplateSettings extends GenericNamedSettings<FileNameTemplateModel> {
    public static final String FEATURE_NAME = "filename-template";
    public static final String DEFAULT_DOCUMENT_NAME_TEMPLATE = "$projectName $document.moduleNameWithSpace.replace(\" / \", \" \")";
    public static final String DEFAULT_REPORT_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") $page.lastRevision";
    public static final String DEFAULT_TESTRUN_NAME_TEMPLATE = "$projectName $testrun.label";
    public static final String DEFAULT_WIKI_NAME_TEMPLATE = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \") $page.lastRevision";

    public FileNameTemplateSettings() {
        super(FEATURE_NAME);
    }

    public FileNameTemplateSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull FileNameTemplateModel defaultValues() {
        return FileNameTemplateModel.builder()
                .documentNameTemplate(DEFAULT_DOCUMENT_NAME_TEMPLATE)
                .reportNameTemplate(DEFAULT_REPORT_NAME_TEMPLATE)
                .testrunNameTemplate(DEFAULT_TESTRUN_NAME_TEMPLATE)
                .wikiNameTemplate(DEFAULT_WIKI_NAME_TEMPLATE)
                .build();
    }
}
