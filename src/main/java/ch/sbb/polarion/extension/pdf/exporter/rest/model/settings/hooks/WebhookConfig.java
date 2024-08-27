package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.hooks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfig {
    private String url;
    private AuthType authType;
    private String authTokenName;
}
