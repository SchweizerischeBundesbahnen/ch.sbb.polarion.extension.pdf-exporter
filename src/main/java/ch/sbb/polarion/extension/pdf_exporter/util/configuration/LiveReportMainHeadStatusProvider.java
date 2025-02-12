package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.alm.projects.properties.internal.ScriptInjectionPropertiesProvider;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class LiveReportMainHeadStatusProvider extends ConfigurationStatusProvider {

    public static final String LIVE_REPORT_BUTTON = "LiveReport Button";
    public static final String LIVE_REPORT_BUTTON_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>(.*)";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        String scriptInjectionSystemPropertiesMainHead = ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().mainHead();
        String scriptInjectionRuntimePropertiesMainHead = ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().mainHead();

        ConfigurationStatus configurationStatusSystemProperties = getConfigurationStatus(LIVE_REPORT_BUTTON, scriptInjectionSystemPropertiesMainHead, LIVE_REPORT_BUTTON_SCRIPT_REGEX);
        ConfigurationStatus configurationStatusRuntimeProperties = getConfigurationStatus(LIVE_REPORT_BUTTON, scriptInjectionRuntimePropertiesMainHead, LIVE_REPORT_BUTTON_SCRIPT_REGEX);

        if (configurationStatusSystemProperties.getStatus() == Status.OK) {
            return configurationStatusSystemProperties;
        } else {
            return configurationStatusRuntimeProperties;
        }
    }
}
