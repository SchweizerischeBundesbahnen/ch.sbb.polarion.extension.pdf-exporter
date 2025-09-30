package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ImageDensity;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WeasyPrintOptions {
    private boolean followHTMLPresentationalHints;
    private PdfVariant pdfVariant;
    private boolean customMetadata;
    private ImageDensity imageDensity;

    public static WeasyPrintOptionsBuilder builder() {
        return new WeasyPrintOptionsBuilder();
    }

    public static class WeasyPrintOptionsBuilder {
        private boolean followHTMLPresentationalHints = false;
        private PdfVariant pdfVariant = PdfVariant.PDF_A_2B;
        private boolean customMetadata = false;
        private ImageDensity imageDensity = ImageDensity.DPI_96;

        public WeasyPrintOptionsBuilder followHTMLPresentationalHints(boolean followHTMLPresentationalHints) {
            this.followHTMLPresentationalHints = followHTMLPresentationalHints;
            return this;
        }

        public WeasyPrintOptionsBuilder pdfVariant(PdfVariant pdfVariant) {
            this.pdfVariant = pdfVariant != null ? pdfVariant : PdfVariant.PDF_A_2B;
            return this;
        }

        public WeasyPrintOptionsBuilder customMetadata(boolean customMetadata) {
            this.customMetadata = customMetadata;
            return this;
        }

        public WeasyPrintOptionsBuilder imageDensity(ImageDensity imageDensity) {
            this.imageDensity = imageDensity != null ? imageDensity : ImageDensity.DPI_96;
            return this;
        }

        public WeasyPrintOptions build() {
            return new WeasyPrintOptions(followHTMLPresentationalHints, pdfVariant, customMetadata, imageDensity);
        }
    }
}
