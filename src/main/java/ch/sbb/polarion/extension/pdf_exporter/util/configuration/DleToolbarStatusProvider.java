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
    // The single-tag injector, the only supported form.
    public static final String DLE_TOOLBAR_SCRIPT_REGEX = "(.*)<script src=\"/polarion/pdf-exporter/js/dle-toolbar.js[^\"]*\"></script>(.*)";
    public static final String NOT_CONFIGURED = "Not configured";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        // Configured in either property source is configured; the system one is not "better" than the
        // runtime one, there is simply one supported form now.
        boolean configured = isConfigured(ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().dleEditorHead())
                || isConfigured(ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().dleEditorHead());
        return configured
                ? new ConfigurationStatus(DLE_TOOLBAR, Status.OK)
                : new ConfigurationStatus(DLE_TOOLBAR, Status.WARNING, NOT_CONFIGURED);
    }

    private boolean isConfigured(@Nullable String dleEditorHead) {
        return dleEditorHead != null && RegexMatcher.get(DLE_TOOLBAR_SCRIPT_REGEX).anyMatch(dleEditorHead);
    }
}
