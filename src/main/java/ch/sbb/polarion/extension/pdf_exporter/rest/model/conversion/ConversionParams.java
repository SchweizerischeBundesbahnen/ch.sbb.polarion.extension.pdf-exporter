package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionParams {

    @Schema(description = "The page orientation of the exported document", defaultValue = "PORTRAIT")
    @Builder.Default
    protected Orientation orientation = Orientation.PORTRAIT;

    @Schema(description = "The paper size of the exported document", defaultValue = "A4")
    @Builder.Default
    protected PaperSize paperSize = PaperSize.A4;

    @Schema(description = "PDF variant to be used for conversion", defaultValue = "PDF_A_2B")
    @Builder.Default
    private PdfVariant pdfVariant = PdfVariant.PDF_A_2B;

    @Schema(description = "Content should be scaled to fit the page", defaultValue = "true")
    @Builder.Default
    protected boolean fitToPage = true;

    @Schema(description = "HTML presentational hints should be followed", defaultValue = "true")
    @Builder.Default
    protected boolean followHTMLPresentationalHints = true;

    @Schema(description = "Target file name of exported document")
    protected String fileName;

    @Schema(description = "Quality of PNG images converted from SVG, by default 96 dpi")
    @Builder.Default
    protected ImageDensity imageDensity = ImageDensity.DPI_96;
}
