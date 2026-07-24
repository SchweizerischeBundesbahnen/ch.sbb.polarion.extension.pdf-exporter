package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.authorization.AuthorizationModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthorizationSettingsTest {

    @Test
    void featureNameIsAuthorization() {
        assertThat(new AuthorizationSettings(mock(SettingsService.class)).getFeatureName()).isEqualTo("authorization");
    }

    @Test
    void defaultValuesAreUnrestricted() {
        AuthorizationModel defaults = new AuthorizationSettings(mock(SettingsService.class)).defaultValues();
        assertThat(defaults.getGlobalRoles()).isEmpty();
        assertThat(defaults.getProjectRoles()).isEmpty();
        assertThat(defaults.isUnrestricted()).isTrue();
    }
}
