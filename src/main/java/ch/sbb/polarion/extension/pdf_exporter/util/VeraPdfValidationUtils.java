package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.ByteArrayInputStream;

@UtilityClass
public class VeraPdfValidationUtils {

    static {
        VeraGreenfieldFoundryProvider.initialise(); // Initialize veraPDF Greenfield foundry provider
    }

    public ValidationResult validatePdf(byte[] pdfBytes, ConversionParams conversionParams) {
        try {
            return validatePdf(pdfBytes, mapPdfVariantToVeraPDFFlavour(conversionParams.getPdfVariant()));
        } catch (Exception e) {
            return null;
        }
    }

    public ValidationResult validatePdf(byte[] pdfBytes, PDFAFlavour flavour) throws Exception {
        try (VeraPDFFoundry veraPDFFoundry = Foundries.defaultInstance();
             PDFAParser parser = veraPDFFoundry
                     .createParser(new ByteArrayInputStream(pdfBytes), flavour)) {

            return veraPDFFoundry
                    .createValidator(flavour, false)
                    .validate(parser);
        }
    }

    public @NotNull PDFAFlavour mapPdfVariantToVeraPDFFlavour(@NotNull PdfVariant pdfVariant) {
        return switch (pdfVariant) {
            case PDF_A_1A -> PDFAFlavour.PDFA_1_A;
            case PDF_A_1B -> PDFAFlavour.PDFA_1_B;
            case PDF_A_2A -> PDFAFlavour.PDFA_2_A;
            case PDF_A_2B -> PDFAFlavour.PDFA_2_B;
            case PDF_A_2U -> PDFAFlavour.PDFA_2_U;
            case PDF_A_3A -> PDFAFlavour.PDFA_3_A;
            case PDF_A_3B -> PDFAFlavour.PDFA_3_B;
            case PDF_A_3U -> PDFAFlavour.PDFA_3_U;
            case PDF_A_4E -> PDFAFlavour.PDFA_4_E;
            case PDF_A_4F -> PDFAFlavour.PDFA_4_F;
            case PDF_A_4U -> PDFAFlavour.PDFA_4;
            case PDF_UA_1 -> PDFAFlavour.PDFUA_1;
            case PDF_UA_2 -> PDFAFlavour.PDFUA_2;
        };
    }


}
