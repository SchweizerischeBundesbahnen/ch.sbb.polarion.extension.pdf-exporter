package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.filename;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
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

    private String documentNameTemplate;
    private String reportNameTemplate;

    @Override
    protected String serializeModelData() {
        return serializeEntry(DOCUMENT_NAME_TEMPLATE, documentNameTemplate) +
                serializeEntry(REPORT_NAME_TEMPLATE, reportNameTemplate);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        documentNameTemplate = deserializeEntry(DOCUMENT_NAME_TEMPLATE, serializedString);
        reportNameTemplate = deserializeEntry(REPORT_NAME_TEMPLATE, serializedString);
    }
}
