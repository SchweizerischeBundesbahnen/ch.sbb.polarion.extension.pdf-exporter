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
public class HooksModel extends SettingsModel {
    public static final String HOOKS = "HOOKS";

    private List<String> hooks = new ArrayList<>();

    @Override
    protected String serializeModelData() {
        return serializeEntry(HOOKS, hooks);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String hooksString = deserializeEntry(HOOKS, serializedString);
        if (hooksString != null) {
            try {
                String[] hooksArray = new ObjectMapper().readValue(hooksString, String[].class);
                hooks = new ArrayList<>(Arrays.asList(hooksArray));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Hooks value couldn't be parsed", e);
            }
        }
    }
}
