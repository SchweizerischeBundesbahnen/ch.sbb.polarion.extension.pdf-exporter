package ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A column of the bulk export widget's table")
public class BulkExportColumn {

    @Schema(description = "Id of the rendered field", example = "status")
    private String id;

    @Schema(description = "Label shown in the table header, resolved when the widget was rendered", example = "Status")
    private String label;
}
