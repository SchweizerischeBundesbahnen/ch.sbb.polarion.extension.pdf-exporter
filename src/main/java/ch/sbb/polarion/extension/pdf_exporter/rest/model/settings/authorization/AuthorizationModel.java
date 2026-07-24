package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.authorization;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.core.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Roles allowed to export documents to PDF. When both lists are empty the export is unrestricted
 * (available to everyone) — this keeps the feature backward compatible for existing installations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationModel extends SettingsModel {
    public static final String GLOBAL_ROLES = "globalRoles";
    public static final String PROJECT_ROLES = "projectRoles";

    protected List<String> globalRoles;
    protected List<String> projectRoles;

    @Override
    protected String serializeModelData() {
        return serializeEntry(GLOBAL_ROLES, serializeRoles(globalRoles)) +
                serializeEntry(PROJECT_ROLES, serializeRoles(projectRoles));
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        globalRoles = deserializeRoles(GLOBAL_ROLES, serializedString);
        projectRoles = deserializeRoles(PROJECT_ROLES, serializedString);
    }

    @NotNull
    protected String serializeRoles(@Nullable List<String> roles) {
        return roles == null ? "" : String.join(",", roles);
    }

    @NotNull
    protected List<String> deserializeRoles(@NotNull String what, @NotNull String serializedString) {
        final String roles = deserializeEntry(what, serializedString);
        return Arrays.stream(roles.split(",")).filter(s -> !StringUtils.isEmpty(s)).map(String::trim).toList();
    }

    @JsonIgnore
    public List<String> getAllRoles() {
        List<String> roles = new ArrayList<>();
        if (globalRoles != null) {
            roles.addAll(globalRoles);
        }
        if (projectRoles != null) {
            roles.addAll(projectRoles);
        }
        return roles;
    }

    @JsonIgnore
    public boolean isUnrestricted() {
        return getAllRoles().isEmpty();
    }
}
