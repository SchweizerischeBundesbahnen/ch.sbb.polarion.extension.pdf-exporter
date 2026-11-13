package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks;

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
    private boolean failExportOnError;

    public WebhookConfig(String url, AuthType authType, String authTokenName) {
        this(url, authType, authTokenName, false);
    }
}
