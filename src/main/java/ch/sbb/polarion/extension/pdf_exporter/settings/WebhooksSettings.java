package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhooksModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WebhooksSettings extends GenericNamedSettings<WebhooksModel> {
    public static final String FEATURE_NAME = "webhooks";

    public WebhooksSettings() {
        super(FEATURE_NAME);
    }

    public WebhooksSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull WebhooksModel defaultValues() {
        return WebhooksModel.builder().webhookConfigs(List.of()).build();
    }
}
