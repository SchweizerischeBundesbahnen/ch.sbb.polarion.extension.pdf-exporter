package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhooksModel extends SettingsModel {
    public static final String WEBHOOKS_ENTRY_NAME = "WEBHOOKS";

    private List<WebhookConfig> webhookConfigs = new ArrayList<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(WEBHOOKS_ENTRY_NAME, webhookConfigs);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String content = deserializeEntry(WEBHOOKS_ENTRY_NAME, serializedString);
        if (content != null) {
            try {
                WebhookConfig[] configs = new ObjectMapper().readValue(content, WebhookConfig[].class);
                this.webhookConfigs = new ArrayList<>(Arrays.asList(configs));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(WEBHOOKS_ENTRY_NAME + " value couldn't be parsed", e);
            }
        }
    }
}
