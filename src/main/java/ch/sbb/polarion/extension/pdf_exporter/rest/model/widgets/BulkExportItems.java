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
@Schema(description = "The table content of a bulk export widget")
public class BulkExportItems {

    @Schema(description = "Columns to render, in order")
    private List<BulkExportColumn> columns;

    @Schema(description = "The rows, already limited to the widget's top value")
    private List<BulkExportItem> items;

    @Schema(description = "How many items the query found, which may exceed the number of rows", example = "9")
    private int totalCount;

    @Schema(description = "The footer message, localized", example = "9 items found")
    private String countMessage;

    @Schema(description = "Link opening the query in Polarion's table view, null when the prototype has no such view")
    private String openInTableUrl;

    @Schema(description = "The query to show behind the footer's info icon")
    private String query;
}
