package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AuthType {
    BEARER_TOKEN,
    XSRF_TOKEN;

    @JsonCreator
    public static AuthType forName(String name) {
        for (AuthType authType : values()) {
            if (authType.name().equalsIgnoreCase(name)) {
                return authType;
            }
        }
        return null;
    }
}
