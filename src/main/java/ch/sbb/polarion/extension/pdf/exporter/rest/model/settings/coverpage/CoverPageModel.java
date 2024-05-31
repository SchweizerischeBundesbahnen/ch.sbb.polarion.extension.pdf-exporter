package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.coverpage;

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

    public static final String TEMPLATE_HTML = "TEMPLATE_HTML";
    public static final String TEMPLATE_CSS = "TEMPLATE_CSS";

    private String templateHtml;
    private String templateCss;

    @Override
    protected String serializeModelData() {
        return serializeEntry(TEMPLATE_HTML, templateHtml) +
                serializeEntry(TEMPLATE_CSS, templateCss);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        templateHtml = deserializeEntry(TEMPLATE_HTML, serializedString);
        templateCss = deserializeEntry(TEMPLATE_CSS, serializedString);
    }


}
