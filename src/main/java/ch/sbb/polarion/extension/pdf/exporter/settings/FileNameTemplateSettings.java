package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.filename.FileNameTemplateModel;
import org.jetbrains.annotations.NotNull;

public class FileNameTemplateSettings extends GenericNamedSettings<FileNameTemplateModel> {
    public static final String FEATURE_NAME = "filename-template";

    public FileNameTemplateSettings() {
        super(FEATURE_NAME);
    }

    public FileNameTemplateSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull FileNameTemplateModel defaultValues() {
        return FileNameTemplateModel.builder()
                .documentNameTemplate("$projectName $document.moduleNameWithSpace.replace(\" / \", \" \")")
                .reportNameTemplate("$projectName $page.pageNameWithSpace.replace(\" / \", \" \") $page.lastRevision")
                .build();
    }
}
