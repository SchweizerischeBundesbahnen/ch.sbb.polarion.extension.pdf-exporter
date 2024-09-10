package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum AuthType {
    BEARER_TOKEN("Bearer"),
    BASIC_AUTH("Basic");

    private final String authHeaderPrefix;

    AuthType(String authHeaderPrefix) {
        this.authHeaderPrefix = authHeaderPrefix;
    }

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
