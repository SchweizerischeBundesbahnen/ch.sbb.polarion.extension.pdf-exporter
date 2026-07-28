package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.authorization.AuthorizationModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AuthorizationSettings extends GenericNamedSettings<AuthorizationModel> {
    public static final String FEATURE_NAME = "authorization";

    public AuthorizationSettings() {
        super(FEATURE_NAME);
    }

    public AuthorizationSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull AuthorizationModel defaultValues() {
        // Empty role lists mean "unrestricted" — every user may export. This keeps PDF export available
        // by default after upgrade; administrators opt into restriction by selecting roles.
        AuthorizationModel authorizationModel = new AuthorizationModel();
        authorizationModel.setGlobalRoles(List.of());
        authorizationModel.setProjectRoles(List.of());
        return authorizationModel;
    }
}
