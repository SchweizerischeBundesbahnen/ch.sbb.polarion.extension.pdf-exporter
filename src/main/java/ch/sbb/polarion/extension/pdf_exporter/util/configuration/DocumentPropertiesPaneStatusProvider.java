package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class DocumentPropertiesPaneStatusProvider extends ConfigurationStatusProvider {

    public static final String DOCUMENT_PROPERTIES_PANE = "Document Properties Pane";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ILocation location = ScopeUtils.getContextLocation(context.getScope()).append(".polarion/documents/sidebar/document.xml");
        String content = new SettingsService().read(location, null);

        if (content == null) {
            location = ScopeUtils.getDefaultLocation().append(".polarion/documents/sidebar/document.xml");
            content = new SettingsService().read(location, null);
            return getConfigurationStatus(DOCUMENT_PROPERTIES_PANE, content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(DOCUMENT_PROPERTIES_PANE, Status.WARNING, "Not configured neither for global scope nor for current project"));
        } else {
            return getConfigurationStatus(DOCUMENT_PROPERTIES_PANE, content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(DOCUMENT_PROPERTIES_PANE, Status.WARNING, "Not configured for current project"));
        }
    }
}
