package ch.sbb.polarion.extension.pdf_exporter.properties;

import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMapping;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDefaultValue;
import ch.sbb.polarion.extension.generic.properties.mappings.PropertyMappingDescription;
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

    public static final String WEBHOOKS_ENABLED = "webhooks.enabled";
    public static final String WEBHOOKS_ENABLED_DESCRIPTION = "Enable <a href='#enabling-webhooks'>webhooks</a>";
    public static final Boolean WEBHOOKS_ENABLED_DEFAULT_VALUE = false;

    @Override
    public String getDebugDescription() {
        return DEBUG_DESCRIPTION;
    }

    @PropertyMapping(WEASYPRINT_SERVICE)
    public String getWeasyPrintService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_SERVICE, WEASYPRINT_SERVICE_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(WEASYPRINT_SERVICE)
    public String getWeasyPrintServiceDescription() {
        return WEASYPRINT_SERVICE_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(WEASYPRINT_SERVICE)
    public String getWeasyPrintServiceDefaultValue() {
        return WEASYPRINT_SERVICE_DEFAULT_VALUE;
    }

    @NotNull
    @PropertyMapping(WEBHOOKS_ENABLED)
    public Boolean getWebhooksEnabled() {
        return SystemValueReader.getInstance().readBoolean(getPropertyPrefix() + WEBHOOKS_ENABLED, WEBHOOKS_ENABLED_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(WEBHOOKS_ENABLED)
    public String getWebhooksEnabledDescription() {
        return WEBHOOKS_ENABLED_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(WEBHOOKS_ENABLED)
    public String getWebhooksEnabledDefaultValue() {
        return String.valueOf(WEBHOOKS_ENABLED_DEFAULT_VALUE);
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(WEBHOOKS_ENABLED);
        return supportedProperties;
    }

    public static PdfExporterExtensionConfiguration getInstance() {
        return (PdfExporterExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
