package ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationStatus {
    private Status status;
    private String details;

    public ConfigurationStatus(Status status) {
        this(status, "");
    }
}
