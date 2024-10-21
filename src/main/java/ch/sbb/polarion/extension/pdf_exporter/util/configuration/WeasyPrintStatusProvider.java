package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Discoverable
public class WeasyPrintStatusProvider extends ConfigurationStatusProvider {

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
            HtmlToPdfConverter htmlToPdfConverter = new HtmlToPdfConverter();
            WeasyPrintServiceConnector weasyPrintServiceConnector = new WeasyPrintServiceConnector();

            WeasyPrintInfo weasyPrintInfo = weasyPrintServiceConnector.getWeasyPrintInfo();
            htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4);

            return List.of(
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION), weasyPrintInfo.getWeasyprintService(), weasyPrintInfo.getTimestamp(), getLatestCompatibleVersionWeasyPrintService()),
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
            return new ConfigurationStatus(name, Status.ERROR, createUpgradeWeasyPrintMessage("Unknown", timestamp, latestCompatibleVersion));
        } else if (!version.equals(latestCompatibleVersion)) {
            return new ConfigurationStatus(name, Status.WARNING, createUpgradeWeasyPrintMessage(version, timestamp, latestCompatibleVersion));
        } else {
            return new ConfigurationStatus(name, Status.OK, version);
        }
    }

    private static @NotNull String createUpgradeWeasyPrintMessage(@NotNull String version, @Nullable String timestamp, @Nullable String latestCompatibleVersion) {
        StringBuilder message = new StringBuilder();
        message.append(version);
        if (timestamp != null && !timestamp.isBlank()) {
            message.append(" (").append(timestamp).append(")");
        }
        if (latestCompatibleVersion != null && !latestCompatibleVersion.isBlank()) {
            message.append(": <span style='color: red;'>upgrade to</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v").append(latestCompatibleVersion).append("' target='_blank'>").append(latestCompatibleVersion).append("</a>");
        }
        return message.toString();
    }

    static @Nullable String getLatestCompatibleVersionWeasyPrintService() {
        try (InputStream input = WeasyPrintStatusProvider.class.getClassLoader().getResourceAsStream("versions.properties")) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("weasyprint-service.version");
        } catch (IOException e) {
            return null;
        }
    }

}
