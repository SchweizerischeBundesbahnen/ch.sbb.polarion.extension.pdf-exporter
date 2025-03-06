package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css;

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
public class CssModel extends SettingsModel {

    public static final String DISABLE_DEFAULT_CSS_ENTRY_NAME = "DISABLE DEFAULT CSS";
    public static final String CSS_ENTRY_NAME = "CSS";

    private boolean disableDefaultCss;
    @Builder.Default
    private String css = "";

    @Override
    protected String serializeModelData() {
        return serializeEntry(DISABLE_DEFAULT_CSS_ENTRY_NAME, disableDefaultCss) +
                serializeEntry(CSS_ENTRY_NAME, css);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        disableDefaultCss = Boolean.parseBoolean(deserializeEntry(DISABLE_DEFAULT_CSS_ENTRY_NAME, serializedString));
        css = deserializeEntry(CSS_ENTRY_NAME, serializedString);
    }
}
