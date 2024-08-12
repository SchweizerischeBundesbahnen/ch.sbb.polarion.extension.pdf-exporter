package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.hooks;

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

    private List<String> webhooks = new ArrayList<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(WEBHOOKS_ENTRY_NAME, webhooks);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String webhooksString = deserializeEntry(WEBHOOKS_ENTRY_NAME, serializedString);
        if (webhooksString != null) {
            try {
                String[] webhooksArray = new ObjectMapper().readValue(webhooksString, String[].class);
                webhooks = new ArrayList<>(Arrays.asList(webhooksArray));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Webhooks value couldn't be parsed", e);
            }
        }
    }
}
