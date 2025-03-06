package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage;

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
public class CoverPageModel extends SettingsModel {

    public static final String USE_CUSTOM_VALUES_ENTRY_NAME = "USE CUSTOM VALUES";
    public static final String TEMPLATE_HTML = "TEMPLATE_HTML";
    public static final String TEMPLATE_CSS = "TEMPLATE_CSS";

    private boolean useCustomValues;
    private String templateHtml;
    private String templateCss;

    @Override
    protected String serializeModelData() {
        return serializeEntry(USE_CUSTOM_VALUES_ENTRY_NAME, useCustomValues) +
                serializeEntry(TEMPLATE_HTML, templateHtml) +
                serializeEntry(TEMPLATE_CSS, templateCss);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        useCustomValues = Boolean.parseBoolean(deserializeEntry(USE_CUSTOM_VALUES_ENTRY_NAME, serializedString));
        templateHtml = deserializeEntry(TEMPLATE_HTML, serializedString);
        templateCss = deserializeEntry(TEMPLATE_CSS, serializedString);
    }


}
