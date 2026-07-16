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
    // Recommended single-tag Live Reports loader.
    public static final String LIVE_REPORT_BUTTON_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/live-reports.js[^\"]*\"></script>(.*)";
    // Deprecated form (loads starter.js, which also drags in the DLE toolbar engine).
    public static final String DEPRECATED_LIVE_REPORT_BUTTON_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>(.*)";
    public static final String NOT_CONFIGURED = "Not configured";
    public static final String DEPRECATED_DETAILS = "Deprecated configuration. Replace it with the single tag "
            + "<script src=\"/polarion/pdf-exporter/js/live-reports.js\"></script>";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ConfigurationStatus systemStatus = classify(ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().mainHead());
        ConfigurationStatus runtimeStatus = classify(ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().mainHead());
        // Prefer the better-configured of the two property sources (system wins on a tie).
        return rank(systemStatus) >= rank(runtimeStatus) ? systemStatus : runtimeStatus;
    }

    private @NotNull ConfigurationStatus classify(@Nullable String mainHead) {
        if (mainHead != null && RegexMatcher.get(LIVE_REPORT_BUTTON_SCRIPT_REGEX).anyMatch(mainHead)) {
            return new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.OK);
        }
        if (mainHead != null && RegexMatcher.get(DEPRECATED_LIVE_REPORT_BUTTON_SCRIPT_REGEX).anyMatch(mainHead)) {
            return new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.WARNING, DEPRECATED_DETAILS);
        }
        return new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.WARNING, NOT_CONFIGURED);
    }

    private int rank(@NotNull ConfigurationStatus status) {
        if (status.getStatus() == Status.OK) {
            return 2;
        }
        return DEPRECATED_DETAILS.equals(status.getDetails()) ? 1 : 0;
    }
}
