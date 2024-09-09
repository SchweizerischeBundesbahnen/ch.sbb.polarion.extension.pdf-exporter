package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.filename;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import ch.sbb.polarion.extension.pdf.exporter.settings.FileNameTemplateSettings;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileNameTemplateModel extends SettingsModel {
    private static final String DOCUMENT_NAME_TEMPLATE = "DOCUMENT NAME TEMPLATE";
    private static final String REPORT_NAME_TEMPLATE = "REPORT NAME TEMPLATE";
    private static final String TESTRUN_NAME_TEMPLATE = "TESTRUN NAME TEMPLATE";
    private static final String WIKI_NAME_TEMPLATE = "WIKI NAME TEMPLATE";

    private String documentNameTemplate;
    private String reportNameTemplate;
    private String testrunNameTemplate;
    private String wikiNameTemplate;

    @Override
    protected String serializeModelData() {
        return serializeEntry(DOCUMENT_NAME_TEMPLATE, documentNameTemplate) +
                serializeEntry(REPORT_NAME_TEMPLATE, reportNameTemplate) +
                serializeEntry(TESTRUN_NAME_TEMPLATE, testrunNameTemplate) +
                serializeEntry(WIKI_NAME_TEMPLATE, wikiNameTemplate);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        documentNameTemplate = deserializeEntry(DOCUMENT_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_DOCUMENT_NAME_TEMPLATE);
        reportNameTemplate = deserializeEntry(REPORT_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_REPORT_NAME_TEMPLATE);
        testrunNameTemplate = deserializeEntry(TESTRUN_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_TESTRUN_NAME_TEMPLATE);
        wikiNameTemplate = deserializeEntry(WIKI_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_WIKI_NAME_TEMPLATE);
    }
}
