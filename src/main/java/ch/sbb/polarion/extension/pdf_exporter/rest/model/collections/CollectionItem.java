package ch.sbb.polarion.extension.pdf_exporter.rest.model.collections;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details about the collection item")
public class CollectionItem {
    @Schema(description = "The name of the module with spaces")
    private String moduleNameWithSpace;

    @Schema(description = "The revision of the module")
    private String revision;
}
