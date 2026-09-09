package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.BulkProcessingServiceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.VERSION_FILE;

@Discoverable
public class BulkProcessingStatusProvider extends ConfigurationStatusProvider {

    private static final String SERVICE_NAME = "Bulk Processing Service";
    private static final String PYTHON_NAME = "Bulk Processing Service: Python";

    private final BulkProcessingServiceConnector connector;

    public BulkProcessingStatusProvider() {
        this.connector = new BulkProcessingServiceConnector();
    }

    public BulkProcessingStatusProvider(BulkProcessingServiceConnector connector) {
        this.connector = connector;
    }

    @Override
    public @NotNull List<ConfigurationStatus> getStatuses(@NotNull Context context) {
        String url = PdfExporterExtensionConfiguration.getInstance().getBulkProcessingService();
        if (url == null || url.isBlank()) {
            return List.of(new ConfigurationStatus(SERVICE_NAME, Status.OK, "Disabled (no URL configured)"));
        }
        try {
            BulkProcessingServiceInfo info = connector.getVersionInfo();
            String expectedApiVersionStr = VersionUtils.getValueFromProperties(VERSION_FILE, "bulk-processing-service.api-version");
            Integer expectedApiVersion = parseExpectedApiVersion(expectedApiVersionStr);
            return List.of(
                    createVersionStatus(info.getBulkProcessingService(), info.getTimestamp(), info.getApiVersion(), expectedApiVersion),
                    createSimpleStatus(PYTHON_NAME, info.getPython())
            );
        } catch (Exception e) {
            return List.of(new ConfigurationStatus(SERVICE_NAME, Status.ERROR, e.getMessage()));
        }
    }

    private static @Nullable Integer parseExpectedApiVersion(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid configuration for 'bulk-processing-service.api-version': '" + value + "' is not a valid integer.", nfe);
        }
    }

    private static @NotNull ConfigurationStatus createSimpleStatus(@NotNull String name, @Nullable String version) {
        if (version == null || version.isBlank()) {
            return new ConfigurationStatus(name, Status.ERROR, "Unknown");
        }
        return new ConfigurationStatus(name, Status.OK, version);
    }

    private static @NotNull ConfigurationStatus createVersionStatus(
            @Nullable String serviceVersion, @Nullable String timestamp,
            @Nullable Integer apiVersion, @Nullable Integer expectedApiVersion) {

        String displayVersion = formatVersionWithTimestamp(serviceVersion, timestamp);

        if (apiVersion == null) {
            return new ConfigurationStatus(SERVICE_NAME, Status.ERROR,
                    displayVersion + ": <span style='color: red;'>API version unknown, please upgrade bulk-processing-service</span>");
        } else if (expectedApiVersion == null) {
            return new ConfigurationStatus(SERVICE_NAME, Status.WARNING,
                    displayVersion + ": <span style='color: orange;'>expected API version not configured</span>");
        } else if (!apiVersion.equals(expectedApiVersion)) {
            return new ConfigurationStatus(SERVICE_NAME, Status.WARNING,
                    displayVersion + ": <span style='color: red;'>incompatible API version " + apiVersion + ", expected " + expectedApiVersion + "</span>");
        }
        return new ConfigurationStatus(SERVICE_NAME, Status.OK, displayVersion);
    }

    private static @NotNull String formatVersionWithTimestamp(@Nullable String version, @Nullable String timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(version != null && !version.isBlank() ? version : "Unknown");
        if (timestamp != null && !timestamp.isBlank()) {
            sb.append(" (").append(timestamp).append(")");
        }
        return sb.toString();
    }
}
