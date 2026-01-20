package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.VERSION_FILE;

@Discoverable
public class WeasyPrintStatusProvider extends ConfigurationStatusProvider {

    private final WeasyPrintServiceConnector weasyPrintServiceConnector;

    public WeasyPrintStatusProvider() {
        this.weasyPrintServiceConnector = new WeasyPrintServiceConnector();
    }

    public WeasyPrintStatusProvider(WeasyPrintServiceConnector weasyPrintServiceConnector) {
        this.weasyPrintServiceConnector = weasyPrintServiceConnector;
    }

    private enum WeasyPrintServiceInfo {
        VERSION,
        PYTHON,
        WEASYPRINT,
        CHROMIUM
    }

    private static final Map<WeasyPrintServiceInfo, String> WEASY_PRINT_SERVICE_INFO = Map.of(
            WeasyPrintServiceInfo.VERSION, "WeasyPrint Service",
            WeasyPrintServiceInfo.PYTHON, "WeasyPrint Service: Python",
            WeasyPrintServiceInfo.WEASYPRINT, "WeasyPrint Service: WeasyPrint",
            WeasyPrintServiceInfo.CHROMIUM, "WeasyPrint Service: Chromium"
    );

    @Override
    public @NotNull List<ConfigurationStatus> getStatuses(@NotNull Context context) {
        try {
            WeasyPrintInfo weasyPrintInfo = weasyPrintServiceConnector.getWeasyPrintInfo();
            String expectedApiVersionStr = VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.api-version");
            Integer expectedApiVersion = expectedApiVersionStr != null ? Integer.valueOf(expectedApiVersionStr) : null;
            return List.of(
                    createWeasyPrintVersionStatus(
                            WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION),
                            weasyPrintInfo.getWeasyprintService(),
                            weasyPrintInfo.getTimestamp(),
                            weasyPrintInfo.getApiVersion(),
                            expectedApiVersion),
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.PYTHON), weasyPrintInfo.getPython()),
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.WEASYPRINT), weasyPrintInfo.getWeasyprint()),
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.CHROMIUM), weasyPrintInfo.getChromium())
            );
        } catch (Exception e) {
            return List.of(new ConfigurationStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION), Status.ERROR, e.getMessage()));
        }
    }

    private static @NotNull ConfigurationStatus createWeasyPrintStatus(@NotNull String name, @Nullable String version) {
        if (version == null || version.isBlank()) {
            return new ConfigurationStatus(name, Status.ERROR, "Unknown");
        } else {
            return new ConfigurationStatus(name, Status.OK, version);
        }
    }

    private static @NotNull ConfigurationStatus createWeasyPrintVersionStatus(
            @NotNull String name,
            @Nullable String serviceVersion,
            @Nullable String timestamp,
            @Nullable Integer apiVersion,
            @Nullable Integer expectedApiVersion) {

        String displayVersion = formatVersionWithTimestamp(serviceVersion, timestamp);

        if (apiVersion == null) {
            return new ConfigurationStatus(name, Status.ERROR,
                    displayVersion + ": <span style='color: red;'>API version unknown, please upgrade weasyprint-service</span>");
        } else if (expectedApiVersion == null) {
            return new ConfigurationStatus(name, Status.WARNING,
                    displayVersion + ": <span style='color: orange;'>expected API version not configured</span>");
        } else if (!apiVersion.equals(expectedApiVersion)) {
            return new ConfigurationStatus(name, Status.WARNING,
                    displayVersion + ": <span style='color: red;'>incompatible API version " + apiVersion + ", expected " + expectedApiVersion + "</span>");
        } else {
            return new ConfigurationStatus(name, Status.OK, displayVersion);
        }
    }

    private static @NotNull String formatVersionWithTimestamp(@Nullable String version, @Nullable String timestamp) {
        StringBuilder message = new StringBuilder();
        message.append(version != null && !version.isBlank() ? version : "Unknown");
        if (timestamp != null && !timestamp.isBlank()) {
            message.append(" (").append(timestamp).append(")");
        }
        return message.toString();
    }

}
