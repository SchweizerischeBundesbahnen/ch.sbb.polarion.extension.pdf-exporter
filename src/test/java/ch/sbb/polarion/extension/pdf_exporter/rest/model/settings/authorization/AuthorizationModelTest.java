package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.authorization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationModelTest {

    @Test
    void freshModelIsUnrestrictedAndNullSafe() {
        AuthorizationModel model = new AuthorizationModel();
        assertThat(model.getAllRoles()).isEmpty();
        assertThat(model.isUnrestricted()).isTrue();
    }

    @Test
    void emptyListsAreUnrestricted() {
        AuthorizationModel model = new AuthorizationModel(List.of(), List.of());
        assertThat(model.isUnrestricted()).isTrue();
        assertThat(model.getAllRoles()).isEmpty();
    }

    @Test
    void getAllRolesCombinesGlobalAndProjectRoles() {
        AuthorizationModel model = new AuthorizationModel(List.of("admin"), List.of("reviewer", "author"));
        assertThat(model.getAllRoles()).containsExactly("admin", "reviewer", "author");
        assertThat(model.isUnrestricted()).isFalse();
    }

    @Test
    void serializeDeserializeRoundTripPreservesRoles() {
        AuthorizationModel original = new AuthorizationModel(List.of("admin", "power-user"), List.of("reviewer"));

        AuthorizationModel restored = new AuthorizationModel();
        restored.deserializeModelData(original.serializeModelData());

        assertThat(restored.getGlobalRoles()).containsExactly("admin", "power-user");
        assertThat(restored.getProjectRoles()).containsExactly("reviewer");
        assertThat(restored.getAllRoles()).containsExactly("admin", "power-user", "reviewer");
    }

    @Test
    void deserializeEmptyRolesYieldsUnrestricted() {
        AuthorizationModel restored = new AuthorizationModel();
        restored.deserializeModelData(new AuthorizationModel(List.of(), List.of()).serializeModelData());
        assertThat(restored.isUnrestricted()).isTrue();
    }
}
