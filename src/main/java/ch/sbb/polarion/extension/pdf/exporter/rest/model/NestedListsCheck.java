package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Check result indicating whether nested lists are present")
public class NestedListsCheck {
    @Schema(description = "Indicates whether the content contains nested lists")
    private boolean containsNestedLists;
}
