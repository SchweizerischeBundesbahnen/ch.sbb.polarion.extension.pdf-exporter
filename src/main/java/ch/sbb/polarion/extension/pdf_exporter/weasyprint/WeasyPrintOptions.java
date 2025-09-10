package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ScaleFactor;
import lombok.Builder;

@Builder
public record WeasyPrintOptions(
        boolean followHTMLPresentationalHints,
        PdfVariant pdfVariant,
        boolean customMetadata,
        ScaleFactor scaleFactor
) {
    public WeasyPrintOptions() {
        this(false, PdfVariant.PDF_A_2B, false, ScaleFactor.DPI_96);
    }
}
