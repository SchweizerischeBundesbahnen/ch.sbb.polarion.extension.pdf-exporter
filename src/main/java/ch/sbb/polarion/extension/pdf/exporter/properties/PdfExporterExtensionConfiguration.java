package ch.sbb.polarion.extension.pdf.exporter.properties;

import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import com.polarion.core.config.impl.SystemValueReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PdfExporterExtensionConfiguration extends ExtensionConfiguration {

    public static final String WEASYPRINT_CONNECTOR = "weasyprint.connector";
    public static final String WEASYPRINT_CONNECTOR_SERVICE = "service";
    public static final String WEASYPRINT_CONNECTOR_CLI = "cli";
    public static final String WEASYPRINT_CONNECTOR_DEFAULT = WEASYPRINT_CONNECTOR_CLI;
    public static final String WEASYPRINT_SERVICE = "weasyprint.service";
    public static final String WEASYPRINT_SERVICE_DEFAULT = "http://localhost:9080";
    public static final String WEASYPRINT_EXECUTABLE = "weasyprint.executable";
    public static final String WEASYPRINT_EXECUTABLE_DEFAULT = "weasyprint";
    public static final String WEASYPRINT_PDF_VARIANT = "weasyprint.pdf.variant";
    public static final String INTERNALIZE_EXTERNAL_CSS = "internalizeExternalCss";

    public WeasyPrintConnector getWeasyprintConnector() {
        String value = SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_CONNECTOR, WEASYPRINT_CONNECTOR_DEFAULT);
        return WeasyPrintConnector.fromString(Objects.requireNonNullElse(value, WEASYPRINT_CONNECTOR_DEFAULT));
    }

    public String getWeasyprintService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_SERVICE, WEASYPRINT_SERVICE_DEFAULT);
    }

    public String getWeasyprintExecutable() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_EXECUTABLE, WEASYPRINT_EXECUTABLE_DEFAULT);
    }

    public String getWeasyprintPdfVariant() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_PDF_VARIANT, null);
    }

    public Boolean getInternalizeExternalCss() {
        return SystemValueReader.getInstance().readBoolean(getPropertyPrefix() + INTERNALIZE_EXTERNAL_CSS, false);
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(WEASYPRINT_CONNECTOR);
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(WEASYPRINT_EXECUTABLE);
        supportedProperties.add(WEASYPRINT_PDF_VARIANT);
        supportedProperties.add(INTERNALIZE_EXTERNAL_CSS);
        return supportedProperties;
    }

    public PdfExporterExtensionConfiguration() {
        super();
    }

    public static PdfExporterExtensionConfiguration getInstance() {
        return PdfExporterExtensionConfigurationHolder.INSTANCE;
    }

    private static class PdfExporterExtensionConfigurationHolder {
        private static final PdfExporterExtensionConfiguration INSTANCE = new PdfExporterExtensionConfiguration();
    }

}
