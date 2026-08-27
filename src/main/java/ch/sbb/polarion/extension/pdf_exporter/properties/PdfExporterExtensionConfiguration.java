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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Discoverable
public class PdfExporterExtensionConfiguration extends ExtensionConfiguration {

    public static final String DEBUG_DESCRIPTION = "Enable <a href='#debug-option'>debug mode</a>";

    public static final String WEASYPRINT_SERVICE = "weasyprint.service";
    public static final String WEASYPRINT_SERVICE_DESCRIPTION = "The URL of the <a href='#weasyprint-configuration'>WeasyPrint service</a>";
    public static final String WEASYPRINT_SERVICE_DEFAULT_VALUE = "http://localhost:9080";

    public static final String BULK_PROCESSING_SERVICE = "bulk.processing.service";
    public static final String BULK_PROCESSING_SERVICE_DESCRIPTION = "The URL of the bulk processing service used for merging multiple documents into a single PDF. Leave blank to disable bulk export.";
    public static final String BULK_PROCESSING_SERVICE_DEFAULT_VALUE = "";
    public static final String WEASYPRINT_API_KEY_SECRET = "weasyprint.apiKeySecret";
    public static final String WEASYPRINT_API_KEY_SECRET_DESCRIPTION = "Name of the Polarion secret holding the <a href='#weasyprint-api-key'>API key of the WeasyPrint service</a>";
    public static final String WEASYPRINT_API_KEY_SECRET_DEFAULT_VALUE = "";

    public static final String WEBHOOKS_ENABLED = "webhooks.enabled";
    public static final String WEBHOOKS_ENABLED_DESCRIPTION = "Enable <a href='#enabling-webhooks'>webhooks</a>";
    public static final Boolean WEBHOOKS_ENABLED_DEFAULT_VALUE = false;

    public static final String RENDERABLE_IMAGE_EXTENSIONS = "renderable.image.extensions";
    public static final String RENDERABLE_IMAGE_EXTENSIONS_DESCRIPTION = "Comma-separated <a href='#renderable-image-extensions'>list of file extensions the exporter can embed as a full-size image</a>";
    protected static final Set<String> RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE = new LinkedHashSet<>(List.of(
            "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "avif", "ico", "cur", "tif", "tiff", "vsdx"
    ));

    public static final String EXTERNAL_RESOURCES_POLICY = "externalResources.policy";
    public static final String EXTERNAL_RESOURCES_POLICY_DESCRIPTION = "BLOCK_INTERNAL, ALLOWLIST_ONLY or ALLOW_ALL: where a document may load <a href='#external-resources'>images, fonts and stylesheets</a> from";
    public static final String EXTERNAL_RESOURCES_POLICY_DEFAULT_VALUE = "BLOCK_INTERNAL";

    public static final String EXTERNAL_RESOURCES_ALLOWED_ORIGINS = "externalResources.allowedOrigins";
    public static final String EXTERNAL_RESOURCES_ALLOWED_ORIGINS_DESCRIPTION = "Comma separated origins, [scheme://]host[:port], which are always allowed as a source of <a href='#external-resources'>external resources</a>";
    public static final String EXTERNAL_RESOURCES_ALLOWED_ORIGINS_DEFAULT_VALUE = "";

    public static final String EXTERNAL_RESOURCES_MAX_SIZE_MB = "externalResources.maxSizeMB";
    public static final String EXTERNAL_RESOURCES_MAX_SIZE_MB_DESCRIPTION = "Size in MB a single loaded <a href='#external-resources'>external resource</a> may reach";
    public static final int EXTERNAL_RESOURCES_MAX_SIZE_MB_DEFAULT_VALUE = 16;

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

    @PropertyMapping(BULK_PROCESSING_SERVICE)
    public String getBulkProcessingService() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + BULK_PROCESSING_SERVICE, BULK_PROCESSING_SERVICE_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(BULK_PROCESSING_SERVICE)
    public String getBulkProcessingServiceDescription() {
        return BULK_PROCESSING_SERVICE_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(BULK_PROCESSING_SERVICE)
    public String getBulkProcessingServiceDefaultValue() {
        return BULK_PROCESSING_SERVICE_DEFAULT_VALUE;
    }

    @PropertyMapping(WEASYPRINT_API_KEY_SECRET)
    public String getWeasyPrintApiKeySecret() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + WEASYPRINT_API_KEY_SECRET, WEASYPRINT_API_KEY_SECRET_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(WEASYPRINT_API_KEY_SECRET)
    public String getWeasyPrintApiKeySecretDescription() {
        return WEASYPRINT_API_KEY_SECRET_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(WEASYPRINT_API_KEY_SECRET)
    public String getWeasyPrintApiKeySecretDefaultValue() {
        return WEASYPRINT_API_KEY_SECRET_DEFAULT_VALUE;
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

    @PropertyMapping(RENDERABLE_IMAGE_EXTENSIONS)
    public String getRenderableImageExtensionsValue() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + RENDERABLE_IMAGE_EXTENSIONS, String.join(", ", RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE));
    }

    @NotNull
    public Set<String> getRenderableImageExtensions() {
        String value = getRenderableImageExtensionsValue();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(RENDERABLE_IMAGE_EXTENSIONS)
    public String getRenderableImageExtensionsDescription() {
        return RENDERABLE_IMAGE_EXTENSIONS_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(RENDERABLE_IMAGE_EXTENSIONS)
    public String getRenderableImageExtensionsDefaultValue() {
        return String.join(", ", RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE);
    }

    @PropertyMapping(EXTERNAL_RESOURCES_POLICY)
    public String getExternalResourcesPolicy() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + EXTERNAL_RESOURCES_POLICY, EXTERNAL_RESOURCES_POLICY_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(EXTERNAL_RESOURCES_POLICY)
    public String getExternalResourcesPolicyDescription() {
        return EXTERNAL_RESOURCES_POLICY_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(EXTERNAL_RESOURCES_POLICY)
    public String getExternalResourcesPolicyDefaultValue() {
        return EXTERNAL_RESOURCES_POLICY_DEFAULT_VALUE;
    }

    @PropertyMapping(EXTERNAL_RESOURCES_ALLOWED_ORIGINS)
    public String getExternalResourcesAllowedOrigins() {
        return SystemValueReader.getInstance().readString(getPropertyPrefix() + EXTERNAL_RESOURCES_ALLOWED_ORIGINS, EXTERNAL_RESOURCES_ALLOWED_ORIGINS_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(EXTERNAL_RESOURCES_ALLOWED_ORIGINS)
    public String getExternalResourcesAllowedOriginsDescription() {
        return EXTERNAL_RESOURCES_ALLOWED_ORIGINS_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(EXTERNAL_RESOURCES_ALLOWED_ORIGINS)
    public String getExternalResourcesAllowedOriginsDefaultValue() {
        return EXTERNAL_RESOURCES_ALLOWED_ORIGINS_DEFAULT_VALUE;
    }

    @PropertyMapping(EXTERNAL_RESOURCES_MAX_SIZE_MB)
    public int getExternalResourcesMaxSizeMB() {
        return SystemValueReader.getInstance().readInt(getPropertyPrefix() + EXTERNAL_RESOURCES_MAX_SIZE_MB, EXTERNAL_RESOURCES_MAX_SIZE_MB_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    @PropertyMappingDescription(EXTERNAL_RESOURCES_MAX_SIZE_MB)
    public String getExternalResourcesMaxSizeMBDescription() {
        return EXTERNAL_RESOURCES_MAX_SIZE_MB_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    @PropertyMappingDefaultValue(EXTERNAL_RESOURCES_MAX_SIZE_MB)
    public String getExternalResourcesMaxSizeMBDefaultValue() {
        return String.valueOf(EXTERNAL_RESOURCES_MAX_SIZE_MB_DEFAULT_VALUE);
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(WEASYPRINT_SERVICE);
        supportedProperties.add(BULK_PROCESSING_SERVICE);
        supportedProperties.add(WEASYPRINT_API_KEY_SECRET);
        supportedProperties.add(WEBHOOKS_ENABLED);
        supportedProperties.add(RENDERABLE_IMAGE_EXTENSIONS);
        supportedProperties.add(EXTERNAL_RESOURCES_POLICY);
        supportedProperties.add(EXTERNAL_RESOURCES_ALLOWED_ORIGINS);
        supportedProperties.add(EXTERNAL_RESOURCES_MAX_SIZE_MB);
        return supportedProperties;
    }

    public static PdfExporterExtensionConfiguration getInstance() {
        return (PdfExporterExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
