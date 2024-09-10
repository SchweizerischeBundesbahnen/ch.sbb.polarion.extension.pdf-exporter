package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.FileNameTemplateSettings;
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
    private static final String LIVE_DOC_NAME_TEMPLATE = "DOCUMENT NAME TEMPLATE";
    private static final String LIVE_REPORT_NAME_TEMPLATE = "REPORT NAME TEMPLATE";
    private static final String TEST_RUN_NAME_TEMPLATE = "TEST RUN NAME TEMPLATE";
    private static final String WIKI_PAGE_NAME_TEMPLATE = "WIKI NAME TEMPLATE";

    private String documentNameTemplate;
    private String reportNameTemplate;
    private String testRunNameTemplate;
    private String wikiNameTemplate;

    @Override
    protected String serializeModelData() {
        return serializeEntry(LIVE_DOC_NAME_TEMPLATE, documentNameTemplate) +
                serializeEntry(LIVE_REPORT_NAME_TEMPLATE, reportNameTemplate) +
                serializeEntry(TEST_RUN_NAME_TEMPLATE, testRunNameTemplate) +
                serializeEntry(WIKI_PAGE_NAME_TEMPLATE, wikiNameTemplate);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        documentNameTemplate = deserializeEntry(LIVE_DOC_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_DOCUMENT_NAME_TEMPLATE);
        reportNameTemplate = deserializeEntry(LIVE_REPORT_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_REPORT_NAME_TEMPLATE);
        testRunNameTemplate = deserializeEntry(TEST_RUN_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_TESTRUN_NAME_TEMPLATE);
        wikiNameTemplate = deserializeEntry(WIKI_PAGE_NAME_TEMPLATE, serializedString, FileNameTemplateSettings.DEFAULT_WIKI_NAME_TEMPLATE);
    }
}
