package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.util.VersionUtils;
import com.polarion.core.config.Configuration;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class PolarionStatusProvider extends ConfigurationStatusProvider {

    public static final String POLARION_ALM = "Polarion ALM";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        String currentCompatibleVersionPolarion = VersionUtils.getCurrentCompatibleVersionPolarion();
        if (currentCompatibleVersionPolarion == null || currentCompatibleVersionPolarion.trim().isEmpty()) {
            return new ConfigurationStatus(POLARION_ALM, Status.ERROR, "Official supported version not set");
        }

        String versionName = Configuration.getInstance().getProduct().versionName();
        if (versionName.startsWith(currentCompatibleVersionPolarion)) {
            return new ConfigurationStatus(POLARION_ALM, Status.OK, versionName);
        } else {
            return new ConfigurationStatus(POLARION_ALM, Status.WARNING, "%s is not official supported".formatted(versionName));
        }
    }
}
