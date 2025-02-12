package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unique document identifier data")
public class DocIdentifier {

    @Schema(description = "Project ID")
    private @Nullable String projectId;

    @Schema(description = "Space ID")
    private @NotNull String spaceId;

    @Schema(description = "Document name")
    private @NotNull String documentName;
}
