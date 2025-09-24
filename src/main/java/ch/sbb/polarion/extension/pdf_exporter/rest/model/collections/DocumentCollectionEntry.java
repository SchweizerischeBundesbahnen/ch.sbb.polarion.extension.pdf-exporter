package ch.sbb.polarion.extension.pdf_exporter.rest.model.collections;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details about document from collection")
public class DocumentCollectionEntry {
    @Schema(description = "The unique identifier for the project", example = "elibrary")
    private String projectId;

    @Schema(description = "The unique identifier of the space within the project")
    private String spaceId;

    @Schema(description = "The name of the document")
    private String documentName;

    @Schema(description = "The revision of the document")
    private String revision;

    @Schema(description = "Target file name of exported document")
    protected String fileName;
}
