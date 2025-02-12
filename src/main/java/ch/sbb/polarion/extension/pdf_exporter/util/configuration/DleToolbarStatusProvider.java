package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.alm.projects.properties.internal.ScriptInjectionPropertiesProvider;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class DleToolbarStatusProvider extends ConfigurationStatusProvider {

    public static final String DLE_TOOLBAR = "DLE Toolbar";
    public static final String DLE_TOOLBAR_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>(.*)<script>PdfExporterStarter.injectToolbar(.*);</script>(.*)";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        String scriptInjectionSystemPropertiesDleEditorHead = ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().dleEditorHead();
        String scriptInjectionRuntimePropertiesDleEditorHead = ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().dleEditorHead();

        ConfigurationStatus configurationStatusSystemProperties = getConfigurationStatus(DLE_TOOLBAR, scriptInjectionSystemPropertiesDleEditorHead, DLE_TOOLBAR_SCRIPT_REGEX);
        ConfigurationStatus configurationStatusRuntimeProperties = getConfigurationStatus(DLE_TOOLBAR, scriptInjectionRuntimePropertiesDleEditorHead, DLE_TOOLBAR_SCRIPT_REGEX);

        if (configurationStatusSystemProperties.getStatus() == Status.OK) {
            return configurationStatusSystemProperties;
        } else {
            return configurationStatusRuntimeProperties;
        }
    }
}
