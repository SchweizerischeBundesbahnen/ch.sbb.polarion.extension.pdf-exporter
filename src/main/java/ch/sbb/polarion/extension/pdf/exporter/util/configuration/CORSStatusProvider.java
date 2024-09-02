package ch.sbb.polarion.extension.pdf.exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.core.config.Configuration;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Discoverable
public class CORSStatusProvider extends ConfigurationStatusProvider {

    public static final String CORS = "CORS (Cross-Origin Resource Sharing)";

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        boolean restEnabled = Configuration.getInstance().rest().enabled();
        if (restEnabled) {
            Set<String> corsAllowedOrigins = Configuration.getInstance().rest().corsAllowedOrigins();
            if (corsAllowedOrigins.isEmpty()) {
                return new ConfigurationStatus(CORS, Status.WARNING, "CORS allowed origins are not configured");
            } else {
                return new ConfigurationStatus(CORS, Status.OK, "CORS allowed origins: %s".formatted(corsAllowedOrigins.stream().toList()));
            }
        } else {
            return new ConfigurationStatus(CORS, Status.WARNING, "Polarion REST API is not enabled, so CORS is not enabled");
        }
    }
}
