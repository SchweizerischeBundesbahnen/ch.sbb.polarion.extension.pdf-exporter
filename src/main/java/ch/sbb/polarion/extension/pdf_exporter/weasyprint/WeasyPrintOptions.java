package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import lombok.Builder;

@Builder
public record WeasyPrintOptions(
        boolean followHTMLPresentationalHints,
        PdfVariant pdfVariant,
        boolean customMetadata,
        double scaleFactor
) {
    public WeasyPrintOptions() {
        this(false, PdfVariant.PDF_A_2B, false, 96);
    }
}
