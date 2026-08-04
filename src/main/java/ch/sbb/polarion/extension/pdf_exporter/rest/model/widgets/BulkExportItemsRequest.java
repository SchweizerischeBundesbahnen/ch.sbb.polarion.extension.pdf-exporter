package ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the widget's shim posts back: the descriptor exactly as the renderer emitted it, plus its signature. Both
 * values are opaque to the browser, which must return them unchanged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A signed request for the rows of a bulk export widget")
public class BulkExportItemsRequest {

    @Schema(description = "Base64url encoded descriptor, as emitted by the widget renderer")
    private String descriptor;

    @Schema(description = "Signature of the descriptor, as emitted by the widget renderer")
    private String signature;
}
