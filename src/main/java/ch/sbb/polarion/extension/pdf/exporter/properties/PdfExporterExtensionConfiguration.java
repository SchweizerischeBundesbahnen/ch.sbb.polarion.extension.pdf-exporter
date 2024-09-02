package ch.sbb.polarion.extension.pdf.exporter.properties;

import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.core.config.impl.SystemValueReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Discoverable
public class PdfExporterExtensionConfiguration extends ExtensionConfiguration {

    public static final String WEASYPRINT_SERVICE = "weasyprint.service";
    public static final String WEASYPRINT_SERVICE_DEFAULT = "http://localhost:9080";
    public static final String WEASYPRINT_PDF_VARIANT = "weasyprint.pdf.variant";
    public static final String INTERNALIZE_EXTERNAL_CSS = "internalizeExternalCss";
    public static final String WEBHOOKS_ENABLED = "webhooks.enabled";

    public String getWeasyprintService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_SERVICE, WEASYPRINT_SERVICE_DEFAULT);
    }

    public String getWeasyprintPdfVariant() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_PDF_VARIANT, null);
    }

    public Boolean getInternalizeExternalCss() {
        return SystemValueReader.getInstance().readBoolean(getPropertyPrefix() + INTERNALIZE_EXTERNAL_CSS, false);
    }

    @NotNull
    public Boolean getWebhooksEnabled() {
        return SystemValueReader.getInstance().readBoolean(getPropertyPrefix() + WEBHOOKS_ENABLED, false);
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(WEASYPRINT_PDF_VARIANT);
        supportedProperties.add(INTERNALIZE_EXTERNAL_CSS);
        supportedProperties.add(WEBHOOKS_ENABLED);
        return supportedProperties;
    }

    public static PdfExporterExtensionConfiguration getInstance() {
        return (PdfExporterExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
