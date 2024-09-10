package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class DefaultSettingsStatusProvider extends ConfigurationStatusProvider {

    public static final String DEFAULT_SETTINGS = "Default Settings";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        ConfigurationStatus configurationStatus = new ConfigurationStatus(DEFAULT_SETTINGS, Status.OK, "");

        NamedSettingsRegistry.INSTANCE.getAll().stream()
                .map(GenericNamedSettings::getFeatureName)
                .forEach(featureName -> {
                    try {
                        NamedSettingsRegistry.INSTANCE.getByFeatureName(featureName).readNames(context.getScope());
                    } catch (Exception e) {
                        configurationStatus.setStatus(Status.ERROR);
                        configurationStatus.setDetails(e.getMessage());
                    }
                });

        return configurationStatus;
    }
}
