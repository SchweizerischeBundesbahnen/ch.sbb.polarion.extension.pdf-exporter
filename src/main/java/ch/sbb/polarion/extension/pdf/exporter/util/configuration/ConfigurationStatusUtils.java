package ch.sbb.polarion.extension.pdf.exporter.util.configuration;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.Status;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.util.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model.WeasyPrintInfo;
import com.polarion.core.config.Configuration;
import com.polarion.subterra.base.location.ILocation;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@UtilityClass
public class ConfigurationStatusUtils {
    public static final String DEFAULT_SETTINGS = "Default Settings";
    public static final String DOCUMENT_PROPERTIES_PANE = "Document Properties Pane";
    public static final String DLE_TOOLBAR = "DLE Toolbar";
    public static final String LIVE_REPORT_BUTTON = "LiveReport Button";
    public static final String CORS = "CORS (Cross-Origin Resource Sharing)";
    public static final List<String> WEASY_PRINT = List.of("WeasyPrint Python", "WeasyPrint", "WeasyPrint Service");

    @SuppressWarnings("java:S1166") //need by design
    public static @NotNull ConfigurationStatus getSettingsStatus(@NotNull String scope) {
        ConfigurationStatus configurationStatus = new ConfigurationStatus(DEFAULT_SETTINGS, Status.OK, "");

        NamedSettingsRegistry.INSTANCE.getAll().stream()
                .map(GenericNamedSettings::getFeatureName)
                .forEach(featureName -> {
                    try {
                        NamedSettingsRegistry.INSTANCE.getByFeatureName(featureName).readNames(scope);
                    } catch (Exception e) {
                        configurationStatus.setStatus(Status.ERROR);
                        configurationStatus.setDetails(e.getMessage());
                    }
                });

        return configurationStatus;
    }

    public static @NotNull ConfigurationStatus getDocumentPropertiesPaneStatus(@NotNull String scope) {
        ILocation location = ScopeUtils.getContextLocation(scope).append(".polarion/documents/sidebar/document.xml");
        String content = new SettingsService().read(location, null);

        if (content == null) {
            location = ScopeUtils.getDefaultLocation().append(".polarion/documents/sidebar/document.xml");
            content = new SettingsService().read(location, null);
            return getConfigurationStatus(DOCUMENT_PROPERTIES_PANE, content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(DOCUMENT_PROPERTIES_PANE, Status.WARNING, "Not configured neither for global scope nor for current project"));
        } else {
            return getConfigurationStatus(DOCUMENT_PROPERTIES_PANE, content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(DOCUMENT_PROPERTIES_PANE, Status.WARNING, "Not configured for current project"));
        }
    }


    public static @NotNull ConfigurationStatus getDleToolbarStatus() {
        final ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        final String content = new SettingsService().read(location, null);

        return getConfigurationStatus(DLE_TOOLBAR, content, "scriptInjection.dleEditorHead=<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>.*<script>PdfExporterStarter.injectToolbar();</script>");
    }

    public static @NotNull ConfigurationStatus getLiveReportMainHeadStatus() {
        final ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        final String content = new SettingsService().read(location, null);

        return getConfigurationStatus(LIVE_REPORT_BUTTON, content, "scriptInjection.mainHead=<script src=\"/polarion/pdf-exporter/js/starter.js\">");
    }

    @SuppressWarnings("java:S1166") //need by design
    public static @NotNull List<ConfigurationStatus> getWeasyPrintStatus() {
        try {
            HtmlToPdfConverter htmlToPdfConverter = new HtmlToPdfConverter();
            WeasyPrintConverter weasyPrintConverter = htmlToPdfConverter.getWeasyPrintConverter();

            WeasyPrintInfo weasyPrintInfo = weasyPrintConverter.getWeasyPrintInfo();
            htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4);

            return List.of(
                    createWeasyPrintStatus(WEASY_PRINT.get(0), weasyPrintInfo.getPython()),
                    createWeasyPrintStatus(WEASY_PRINT.get(1), weasyPrintInfo.getWeasyprint()),
                    createWeasyPrintStatus(WEASY_PRINT.get(2), weasyPrintInfo.getWeasyprintService())
            );
        } catch (Exception e) {
            return List.of(new ConfigurationStatus(WEASY_PRINT.get(0), Status.ERROR, e.getMessage()));
        }
    }

    private static @NotNull ConfigurationStatus createWeasyPrintStatus(@NotNull String name, @Nullable String description) {
        if (description == null) {
            return new ConfigurationStatus(name, Status.WARNING, "Unknown");
        } else {
            return new ConfigurationStatus(name, Status.OK, description);
        }
    }

    public static @NotNull ConfigurationStatus getCORSStatus() {
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

    private static @NotNull ConfigurationStatus getConfigurationStatus(@NotNull String name, @Nullable String content, @NotNull String regex) {
        return getConfigurationStatus(name, content, regex, new ConfigurationStatus(name, Status.WARNING, "Not configured"));
    }

    private static @NotNull ConfigurationStatus getConfigurationStatus(@NotNull String name, @Nullable String content, @NotNull String regex, @NotNull ConfigurationStatus notOkStatus) {
        if (content != null && contains(content, regex)) {
            return new ConfigurationStatus(name, Status.OK);
        } else {
            return notOkStatus;
        }
    }

    private static boolean contains(@NotNull String input, @NotNull String regex) {
        return RegexMatcher.get(regex).anyMatch(input);
    }
}
