package ch.sbb.polarion.extension.pdf.exporter.util.configuration;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.Status;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.util.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.core.config.Configuration;
import com.polarion.subterra.base.location.ILocation;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.module.ModuleDescriptor;
import java.util.Set;

@UtilityClass
public class ConfigurationStatusUtils {
    @SuppressWarnings("java:S1166") //need by design
    public static @NotNull ConfigurationStatus getSettingsStatus(@NotNull String scope) {
        ConfigurationStatus configurationStatus = new ConfigurationStatus(Status.OK, "");

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
            return getConfigurationStatus(content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(Status.WARNING, "Not configured neither for global scope nor for current project"));
        } else {
            return getConfigurationStatus(content, "<extension id=\"pdf-exporter\".*/>", new ConfigurationStatus(Status.WARNING, "Not configured for current project"));
        }
    }


    public static @NotNull ConfigurationStatus getDleToolbarStatus() {
        final ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        final String content = new SettingsService().read(location, null);

        return getConfigurationStatus(content, "scriptInjection.dleEditorHead=<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>.*<script>PdfExporterStarter.injectToolbar();</script>");
    }

    public static @NotNull ConfigurationStatus getLiveReportMainHeadStatus() {
        final ILocation location = ScopeUtils.getDefaultLocation().append(".polarion/context.properties");
        final String content = new SettingsService().read(location, null);

        return getConfigurationStatus(content, "scriptInjection.mainHead=<script src=\"/polarion/pdf-exporter/js/starter.js\">");
    }

    @SuppressWarnings("java:S1166") //need by design
    public static @NotNull ConfigurationStatus getWeasyPrintStatus() {
        try {
            HtmlToPdfConverter htmlToPdfConverter = new HtmlToPdfConverter();
            WeasyPrintConverter weasyPrintConverter = htmlToPdfConverter.getWeasyPrintConverter();

            ModuleDescriptor.Version weasyPrintVersion = weasyPrintConverter.getWeasyPrintVersion();
            htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4);
            return new ConfigurationStatus(Status.OK, "Version: %s".formatted(weasyPrintVersion.toString()));
        } catch (Exception e) {
            return new ConfigurationStatus(Status.ERROR, e.getMessage());
        }
    }

    public static @NotNull ConfigurationStatus getWeasyPrintServiceStatus() {
        try {
            HtmlToPdfConverter htmlToPdfConverter = new HtmlToPdfConverter();
            WeasyPrintConverter weasyPrintConverter = htmlToPdfConverter.getWeasyPrintConverter();
            if (weasyPrintConverter instanceof WeasyPrintServiceConnector) {
                ModuleDescriptor.Version weasyPrintServiceVersion = ((WeasyPrintServiceConnector) weasyPrintConverter).getWeasyPrintServiceVersion();
                return new ConfigurationStatus(Status.OK, "Version: %s".formatted(weasyPrintServiceVersion.toString()));
            } else {
                return new ConfigurationStatus(Status.WARNING, "WeasyPrint Service not configured");
            }
        } catch (Exception e) {
            return new ConfigurationStatus(Status.ERROR, e.getMessage());
        }
    }

    public static @NotNull ConfigurationStatus getCORSStatus() {
        boolean restEnabled = Configuration.getInstance().rest().enabled();
        if (restEnabled) {
            Set<String> corsAllowedOrigins = Configuration.getInstance().rest().corsAllowedOrigins();
            if (corsAllowedOrigins.isEmpty()) {
                return new ConfigurationStatus(Status.WARNING, "CORS allowed origins are not configured");
            } else {
                return new ConfigurationStatus(Status.OK, "CORS allowed origins: %s".formatted(corsAllowedOrigins.stream().toList()));
            }
        } else {
            return new ConfigurationStatus(Status.WARNING, "Polarion REST API is not enabled, so CORS is not enabled");
        }
    }

    private static @NotNull ConfigurationStatus getConfigurationStatus(@Nullable String content, @NotNull String regex) {
        return getConfigurationStatus(content, regex, new ConfigurationStatus(Status.WARNING, "Not configured"));
    }

    private static @NotNull ConfigurationStatus getConfigurationStatus(@Nullable String content, @NotNull String regex, @NotNull ConfigurationStatus notOkStatus) {
        if (content != null && contains(content, regex)) {
            return new ConfigurationStatus(Status.OK);
        } else {
            return notOkStatus;
        }
    }

    private static boolean contains(@NotNull String input, @NotNull String regex) {
        return RegexMatcher.get(regex).anyMatch(input);
    }
}
