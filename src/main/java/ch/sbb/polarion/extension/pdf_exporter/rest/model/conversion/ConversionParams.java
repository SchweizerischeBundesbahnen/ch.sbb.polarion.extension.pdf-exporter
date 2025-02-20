package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionParams {

    @Schema(description = "The page orientation of the exported document", defaultValue = "PORTRAIT")
    protected Orientation orientation = Orientation.PORTRAIT;

    @Schema(description = "The paper size of the exported document", defaultValue = "A4")
    protected PaperSize paperSize = PaperSize.A4;

    @Schema(description = "Content should be scaled to fit the page", defaultValue = "true")
    protected boolean fitToPage = true;

    @Schema(description = "HTML presentational hints should be followed", defaultValue = "true")
    protected boolean followHTMLPresentationalHints = true;

    @Schema(description = "Target file name of exported document")
    protected String fileName;
}
