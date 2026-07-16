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
public class DleToolbarStatusProvider extends ConfigurationStatusProvider {

    public static final String DLE_TOOLBAR = "DLE Toolbar";
    // Recommended single-tag injector.
    public static final String DLE_TOOLBAR_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/dle-toolbar.js[^\"]*\"></script>(.*)";
    // Deprecated explicit-injectToolbar config (still works).
    public static final String DEPRECATED_DLE_TOOLBAR_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/starter.js[^\"]*\"></script>(.*)<script>PdfExporterStarter.injectToolbar(.*);</script>(.*)";
    public static final String NOT_CONFIGURED = "Not configured";
    public static final String DEPRECATED_DETAILS = "Deprecated configuration. Replace it with the single tag "
            + "<script src=\"/polarion/pdf-exporter/js/dle-toolbar.js\"></script>";

    // Ordered best-to-worst; when the two property sources disagree the lower ordinal wins.
    private enum ConfigForm {
        RECOMMENDED, DEPRECATED, MISSING
    }

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ConfigForm system = evaluate(ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().dleEditorHead());
        ConfigForm runtime = evaluate(ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().dleEditorHead());
        // Prefer the better-configured source (system wins on a tie).
        return toStatus(system.ordinal() <= runtime.ordinal() ? system : runtime);
    }

    private @NotNull ConfigForm evaluate(@Nullable String dleEditorHead) {
        if (dleEditorHead == null) {
            return ConfigForm.MISSING;
        }
        if (RegexMatcher.get(DLE_TOOLBAR_SCRIPT_REGEX).anyMatch(dleEditorHead)) {
            return ConfigForm.RECOMMENDED;
        }
        if (RegexMatcher.get(DEPRECATED_DLE_TOOLBAR_SCRIPT_REGEX).anyMatch(dleEditorHead)) {
            return ConfigForm.DEPRECATED;
        }
        return ConfigForm.MISSING;
    }

    private @NotNull ConfigurationStatus toStatus(@NotNull ConfigForm form) {
        return switch (form) {
            case RECOMMENDED -> new ConfigurationStatus(DLE_TOOLBAR, Status.OK);
            case DEPRECATED -> new ConfigurationStatus(DLE_TOOLBAR, Status.WARNING, DEPRECATED_DETAILS);
            case MISSING -> new ConfigurationStatus(DLE_TOOLBAR, Status.WARNING, NOT_CONFIGURED);
        };
    }
}
