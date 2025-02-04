package ch.sbb.polarion.extension.pdf_exporter.properties;

import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.core.config.impl.SystemValueReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Discoverable
public class PdfExporterExtensionConfiguration extends ExtensionConfiguration {

    public static final String DEBUG_DESCRIPTION = "Enable <a href='#debug-option'>debug mode</a>";

    public static final String WEASYPRINT_SERVICE = "weasyprint.service";
    public static final String WEASYPRINT_SERVICE_DESCRIPTION = "The URL of the <a href='#weasyprint-configuration'>WeasyPrint service</a>";
    public static final String WEASYPRINT_SERVICE_DEFAULT_VALUE = "http://localhost:9080";

    public static final String WEASYPRINT_PDF_VARIANT = "weasyprint.pdf.variant";
    public static final String WEASYPRINT_PDF_VARIANT_DESCRIPTION = "The <a href='#pdf-variants-configuration'>PDF variant</a> of generated PDF files";
    public static final String WEASYPRINT_PDF_VARIANT_DEFAULT_VALUE = "pdf/a-2b";

    public static final String PANDOC_SERVICE = "pandoc.service";
    public static final String PANDOC_SERVICE_DESCRIPTION = "The URL of the <a href='#pandoc-configuration'>Pandoc service</a>";
    public static final String PANDOC_SERVICE_DEFAULT_VALUE = "http://localhost:9090";

    public static final String WEBHOOKS_ENABLED = "webhooks.enabled";
    public static final String WEBHOOKS_ENABLED_DESCRIPTION = "Enable <a href='#enabling-webhooks'>webhooks</a>";
    public static final Boolean WEBHOOKS_ENABLED_DEFAULT_VALUE = false;

    @Override
    public String getDebugDescription() {
        return DEBUG_DESCRIPTION;
    }

    public String getWeasyprintService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_SERVICE, WEASYPRINT_SERVICE_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getWeasyprintServiceDescription() {
        return WEASYPRINT_SERVICE_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public String getWeasyprintServiceDefaultValue() {
        return WEASYPRINT_SERVICE_DEFAULT_VALUE;
    }

    public String getWeasyprintPdfVariant() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_PDF_VARIANT, WEASYPRINT_PDF_VARIANT_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getWeasyprintPdfVariantDescription() {
        return WEASYPRINT_PDF_VARIANT_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public String getWeasyprintPdfVariantDefaultValue() {
        return WEASYPRINT_PDF_VARIANT_DEFAULT_VALUE;
    }

    public String getPandocService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + PANDOC_SERVICE, PANDOC_SERVICE_DEFAULT_VALUE);
    }

    @NotNull
    public Boolean getWebhooksEnabled() {
        return SystemValueReader.getInstance().readBoolean(getPropertyPrefix() + WEBHOOKS_ENABLED, WEBHOOKS_ENABLED_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getWebhooksEnabledDescription() {
        return WEBHOOKS_ENABLED_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public String getWebhooksEnabledDefaultValue() {
        return String.valueOf(WEBHOOKS_ENABLED_DEFAULT_VALUE);
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(WEASYPRINT_PDF_VARIANT);
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(PANDOC_SERVICE);
        return supportedProperties;
    }

    public static PdfExporterExtensionConfiguration getInstance() {
        return (PdfExporterExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
