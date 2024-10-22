package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.util.VersionsUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
            return List.of(
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION), weasyPrintInfo.getWeasyprintService(), weasyPrintInfo.getTimestamp(), VersionsUtils.getLatestCompatibleVersionWeasyPrintService()),
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

    private static @NotNull ConfigurationStatus createWeasyPrintStatus(@NotNull String name, @Nullable String version, @Nullable String timestamp, @Nullable String latestCompatibleVersion) {
        if (version == null || version.isBlank()) {
            return new ConfigurationStatus(name, Status.ERROR, createUseLatestCompatibleWeasyPrintMessage("Unknown", timestamp, latestCompatibleVersion));
        } else if (!version.equals(latestCompatibleVersion)) {
            return new ConfigurationStatus(name, Status.WARNING, createUseLatestCompatibleWeasyPrintMessage(version, timestamp, latestCompatibleVersion));
        } else {
            return new ConfigurationStatus(name, Status.OK, version);
        }
    }

    private static @NotNull String createUseLatestCompatibleWeasyPrintMessage(@NotNull String version, @Nullable String timestamp, @Nullable String latestCompatibleVersion) {
        StringBuilder message = new StringBuilder();
        message.append(version);
        if (timestamp != null && !timestamp.isBlank()) {
            message.append(" (").append(timestamp).append(")");
        }
        if (latestCompatibleVersion != null && !latestCompatibleVersion.isBlank()) {
            message.append(": <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v").append(latestCompatibleVersion).append("' target='_blank'>").append(latestCompatibleVersion).append("</a>");
        }
        return message.toString();
    }

}
