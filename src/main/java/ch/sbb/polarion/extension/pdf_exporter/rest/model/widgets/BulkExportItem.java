package ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One row of the bulk export widget's table")
public class BulkExportItem {

    @Schema(description = "Whether the current user may read the item. An unreadable row shows only its message")
    private boolean readable;

    @Schema(description = "Why the row is not readable, localized", example = "You do not have permission to read this item")
    private String message;

    @Schema(description = "Name of the item's Polarion prototype", example = "TestRun")
    private String type;

    @Schema(description = "Id of the project the item belongs to", example = "elibrary")
    private String projectId;

    @Schema(description = "Id of the space the item belongs to, for documents and pages")
    private String spaceId;

    @Schema(description = "Id of the item", example = "build_quick-20170211-141155")
    private String id;

    @Schema(description = "Name of the item, for baseline collections")
    private String name;

    @Schema(description = "The item's cells, rendered by Polarion in the order of the widget's columns")
    private List<String> cells;
}
