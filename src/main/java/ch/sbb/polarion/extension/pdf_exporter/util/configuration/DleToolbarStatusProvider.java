package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class DleToolbarStatusProvider extends ConfigurationStatusProvider {

    public static final String DLE_TOOLBAR = "DLE Toolbar";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        String content = new SettingsService().read(location, null);

        return getConfigurationStatus(DLE_TOOLBAR, content, "scriptInjection.dleEditorHead=<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>.*<script>PdfExporterStarter.injectToolbar();</script>");
    }
}
