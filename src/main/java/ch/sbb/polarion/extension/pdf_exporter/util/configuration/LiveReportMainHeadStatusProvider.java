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
    public static final String DEPRECATED_LIVE_REPORT_BUTTON_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/starter.js[^\"]*\"></script>(.*)";
    public static final String NOT_CONFIGURED = "Not configured";
    public static final String DEPRECATED_DETAILS = "Deprecated configuration. Replace it with the single tag "
            + "<script src=\"/polarion/pdf-exporter/js/live-reports.js\"></script>";

    // Ordered best-to-worst; when the two property sources disagree the lower ordinal wins.
    private enum ConfigForm {
        RECOMMENDED, DEPRECATED, MISSING
    }

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ConfigForm system = evaluate(ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().mainHead());
        ConfigForm runtime = evaluate(ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().mainHead());
        // Prefer the better-configured source (system wins on a tie).
        return toStatus(system.ordinal() <= runtime.ordinal() ? system : runtime);
    }

    private @NotNull ConfigForm evaluate(@Nullable String mainHead) {
        if (mainHead == null) {
            return ConfigForm.MISSING;
        }
        if (RegexMatcher.get(LIVE_REPORT_BUTTON_SCRIPT_REGEX).anyMatch(mainHead)) {
            return ConfigForm.RECOMMENDED;
        }
        if (RegexMatcher.get(DEPRECATED_LIVE_REPORT_BUTTON_SCRIPT_REGEX).anyMatch(mainHead)) {
            return ConfigForm.DEPRECATED;
        }
        return ConfigForm.MISSING;
    }

    private @NotNull ConfigurationStatus toStatus(@NotNull ConfigForm form) {
        return switch (form) {
            case RECOMMENDED -> new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.OK);
            case DEPRECATED -> new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.WARNING, DEPRECATED_DETAILS);
            case MISSING -> new ConfigurationStatus(LIVE_REPORT_BUTTON, Status.WARNING, NOT_CONFIGURED);
        };
    }
}
