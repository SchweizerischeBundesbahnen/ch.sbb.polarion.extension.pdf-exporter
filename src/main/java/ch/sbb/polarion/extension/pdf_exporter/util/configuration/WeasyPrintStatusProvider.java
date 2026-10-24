package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.Map;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.VERSION_FILE;

@Discoverable
public class WeasyPrintStatusProvider extends ConfigurationStatusProvider {

    private final WeasyPrintServiceConnector weasyPrintServiceConnector;

    private final @NotNull Supplier<String> apiKeySecretName;

    public WeasyPrintStatusProvider() {
        this(new WeasyPrintServiceConnector(), () -> PdfExporterExtensionConfiguration.getInstance().getWeasyPrintApiKeySecret());
    }

    /**
     * Names no secret, so the transport of a key is not judged. Reading the configuration needs a
     * running extension context, which a plain unit test does not have.
     */
    @VisibleForTesting
    public WeasyPrintStatusProvider(WeasyPrintServiceConnector weasyPrintServiceConnector) {
        this(weasyPrintServiceConnector, () -> null);
    }

    @VisibleForTesting
    public WeasyPrintStatusProvider(WeasyPrintServiceConnector weasyPrintServiceConnector, @NotNull Supplier<String> apiKeySecretName) {
        this.weasyPrintServiceConnector = weasyPrintServiceConnector;
        this.apiKeySecretName = apiKeySecretName;
    }

    private enum WeasyPrintServiceInfo {
        VERSION,
        PYTHON,
        WEASYPRINT,
        CHROMIUM,
        SVG_RENDERER,
        VISIO
    }

    private static final String WEASYSVG = "weasysvg";

    private static final Map<WeasyPrintServiceInfo, String> WEASY_PRINT_SERVICE_INFO = Map.of(
            WeasyPrintServiceInfo.VERSION, "WeasyPrint Service",
            WeasyPrintServiceInfo.PYTHON, "WeasyPrint Service: Python",
            WeasyPrintServiceInfo.WEASYPRINT, "WeasyPrint Service: WeasyPrint",
            WeasyPrintServiceInfo.CHROMIUM, "WeasyPrint Service: Chromium",
            WeasyPrintServiceInfo.SVG_RENDERER, "WeasyPrint Service: SVG renderer",
            WeasyPrintServiceInfo.VISIO, "WeasyPrint Service: Visio drawings"
    );

    @Override
    public @NotNull List<ConfigurationStatus> getStatuses(@NotNull Context context) {
        ConfigurationStatus transportStatus = apiKeyTransportStatus();
        if (transportStatus != null) {
            // reported here rather than at the first export: /version carries no key, so the service
            // answers happily while every export is refused
            return List.of(transportStatus);
        }
        try {
            WeasyPrintInfo weasyPrintInfo = weasyPrintServiceConnector.getWeasyPrintInfo();
            String expectedApiVersionStr = VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.api-version");
            Integer expectedApiVersion = parseExpectedApiVersion(expectedApiVersionStr);
            List<ConfigurationStatus> statuses = new ArrayList<>(List.of(
                    createWeasyPrintVersionStatus(
                            WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION),
                            weasyPrintInfo.getWeasyprintService(),
                            weasyPrintInfo.getTimestamp(),
                            weasyPrintInfo.getApiVersion(),
                            expectedApiVersion),
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.PYTHON), weasyPrintInfo.getPython()),
                    createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.WEASYPRINT), weasyPrintInfo.getWeasyprint())));
            String svgRenderer = weasyPrintInfo.getSvgRenderer();
            // with weasysvg the service starts no browser and reports no Chromium, which is no error
            if (!WEASYSVG.equals(svgRenderer)) {
                statuses.add(createWeasyPrintStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.CHROMIUM), weasyPrintInfo.getChromium()));
            }
            // a service which names its SVG renderer is recent enough to also report what draws Visio drawings
            if (svgRenderer != null) {
                statuses.add(createSvgRendererStatus(svgRenderer, weasyPrintInfo.getWeasysvg()));
                statuses.add(createVisioStatus(weasyPrintInfo.getWeasyvsdx(), weasyPrintInfo.getLibvisio()));
            }
            return statuses;
        } catch (Exception e) {
            return List.of(new ConfigurationStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION), Status.ERROR, e.getMessage()));
        }
    }

    /**
     * @return what to show where a key is configured for a service named over plain http, null otherwise
     */
    private @Nullable ConfigurationStatus apiKeyTransportStatus() {
        String secretName = apiKeySecretName.get();
        if (secretName == null || secretName.isBlank()) {
            return null;
        }
        if (weasyPrintServiceConnector.getWeasyPrintServiceBaseUrl().toLowerCase(Locale.ROOT).startsWith("https://")) {
            return null;
        }
        return new ConfigurationStatus(WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VERSION), Status.ERROR,
                String.format("An API key is configured in '%s', but '%s' names a plain http address. The key is not sent over http, so every export is refused. Name the service with an https address.",
                        PdfExporterExtensionConfiguration.WEASYPRINT_API_KEY_SECRET, PdfExporterExtensionConfiguration.WEASYPRINT_SERVICE));
    }

    private static @Nullable Integer parseExpectedApiVersion(@Nullable String expectedApiVersionStr) {
        if (expectedApiVersionStr == null) {
            return null;
        }
        try {
            return Integer.valueOf(expectedApiVersionStr);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    "Invalid configuration for 'weasyprint-service.api-version': '" + expectedApiVersionStr + "' is not a valid integer.",
                    nfe
            );
        }
    }

    private static @NotNull ConfigurationStatus createSvgRendererStatus(@NotNull String svgRenderer, @Nullable String weasysvg) {
        String name = WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.SVG_RENDERER);
        if (WEASYSVG.equals(svgRenderer) && weasysvg != null && !weasysvg.isBlank()) {
            return new ConfigurationStatus(name, Status.OK, WEASYSVG + " " + weasysvg);
        }
        return new ConfigurationStatus(name, Status.OK, svgRenderer);
    }

    private static @NotNull ConfigurationStatus createVisioStatus(@Nullable String weasyvsdx, @Nullable String libvisio) {
        String name = WEASY_PRINT_SERVICE_INFO.get(WeasyPrintServiceInfo.VISIO);
        if (weasyvsdx == null || weasyvsdx.isBlank()) {
            return new ConfigurationStatus(name, Status.WARNING,
                    "<span style='color: orange;'>not available: Visio drawings are left out of the PDF. weasyprint-service draws them from 70.0.7 on, with libvisio-tools in its image</span>");
        }
        String libvisioVersion = libvisio != null && !libvisio.isBlank() ? libvisio : "unknown";
        return new ConfigurationStatus(name, Status.OK, "weasyvsdx " + weasyvsdx + ", libvisio " + libvisioVersion);
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
