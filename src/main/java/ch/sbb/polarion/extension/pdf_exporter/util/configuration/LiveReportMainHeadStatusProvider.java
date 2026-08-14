package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.alm.projects.properties.internal.ScriptInjectionPropertiesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Discoverable
public class LiveReportMainHeadStatusProvider extends ConfigurationStatusProvider {

    public static final String LIVE_REPORT_BUTTON = "LiveReport Button";
    // Recommended single-tag Live Reports loader; extra attributes (e.g. data-expand-tools) allowed.
    public static final String LIVE_REPORT_BUTTON_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/live-reports.js[^\"]*\"[^>]*></script>(.*)";
    public static final String NOT_CONFIGURED = "Not configured";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        // Configured in either property source is configured; the system one is not "better" than the
        // runtime one, there is simply one supported form now.
        boolean configured = isConfigured(ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().mainHead())
                || isConfigured(ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().mainHead());
        return configured
                ? new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.OK)
                : new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.WARNING, NOT_CONFIGURED);
    }

    private boolean isConfigured(@Nullable String mainHead) {
        return mainHead != null && RegexMatcher.get(LIVE_REPORT_BUTTON_SCRIPT_REGEX).anyMatch(mainHead);
    }
}
