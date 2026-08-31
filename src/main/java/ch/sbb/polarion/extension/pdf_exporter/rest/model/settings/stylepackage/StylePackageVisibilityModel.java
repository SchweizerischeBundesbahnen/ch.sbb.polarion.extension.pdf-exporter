package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Which style packages a scope offers, one document per scope. It sits above the style packages rather
 * than inside one of them, and the global document provides the values every project uses unless the
 * project saves its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePackageVisibilityModel extends SettingsModel {
    private static final String HIDE_GLOBAL_STYLE_PACKAGES_ENTRY_NAME = "HIDE GLOBAL STYLE PACKAGES";

    /**
     * When set on a project scope, style packages defined on the global level are neither offered on the
     * export dialogs of that project nor usable by its name. Defaults to false, which keeps the style
     * packages of the global level inherited by every project.
     */
    private boolean hideGlobalStylePackages;

    @Override
    protected String serializeModelData() {
        return serializeEntry(HIDE_GLOBAL_STYLE_PACKAGES_ENTRY_NAME, hideGlobalStylePackages);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        hideGlobalStylePackages = Boolean.parseBoolean(deserializeEntry(HIDE_GLOBAL_STYLE_PACKAGES_ENTRY_NAME, serializedString));
    }
}
