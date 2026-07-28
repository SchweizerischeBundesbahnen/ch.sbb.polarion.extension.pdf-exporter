package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PlatformContextMockExtension.class)
class RolesUtilsTest {

    @Test
    void getGlobalRolesDelegatesToSecurityService() {
        assertThat(RolesUtils.getGlobalRoles()).isNotNull();
    }

    @Test
    void getProjectRolesReturnsEmptyForScopeWithoutProject() {
        // A non-project (global/repository) scope has no project id → no project roles.
        assertThat(RolesUtils.getProjectRoles("")).isEmpty();
    }

    @Test
    void getProjectRolesResolvesProjectForProjectScope() {
        assertThat(RolesUtils.getProjectRoles("project/myProject/")).isNotNull();
    }
}
