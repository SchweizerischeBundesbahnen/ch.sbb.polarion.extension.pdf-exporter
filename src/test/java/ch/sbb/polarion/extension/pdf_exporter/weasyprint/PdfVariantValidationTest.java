package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for PDF variant validation using veraPDF library.
 * Tests supported PDF variants (PDF/A-1b, PDF/A-2b, PDF/A-3b, PDF/A-2u, PDF/A-3u, PDF/UA-1)
 * by converting HTML with images to PDF and validating the result.
 * Note: PDF/A-1b, PDF/A-4b and PDF/A-4u are currently excluded from testing.
 */
@ExtendWith({CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class PdfVariantValidationTest extends BaseWeasyPrintTest {

    static {
        VeraGreenfieldFoundryProvider.initialise(); // Initialize veraPDF Greenfield foundry provider
    }

    @ParameterizedTest(name = "Test PDF conversion and validation for {0}")
    @EnumSource(
            value = PdfVariant.class,
            mode = EnumSource.Mode.EXCLUDE, names = {"PDF_A_1B", "PDF_A_4B", "PDF_A_4U"}
    )
    @SneakyThrows
    void testPdfVariantConversionAndValidation(PdfVariant pdfVariant) {
        // Read HTML resource with images
        String html = readHtmlResource("pdfVariantValidation");
        assertNotNull(html, "HTML resource should not be null");

        // Inject default CSS into HTML
        html = injectDefaultCss(html);

        // Convert HTML to PDF with the specified PDF variant
        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(pdfVariant)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");

        // Write PDF to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, pdfVariant.name(), pdfBytes);

        // Validate PDF using veraPDF
        PDFAFlavour flavour = mapPdfVariantToVeraPDFFlavour(pdfVariant);
        ValidationResult result = validatePdf(pdfBytes, flavour);

        assertTrue(result.isCompliant(), String.format("PDF must be compliant with %s (VeraPDF flavour: %s). Failed rules: %s", pdfVariant, flavour, result.getTestAssertions()));
    }

    private ValidationResult validatePdf(byte[] pdfBytes, PDFAFlavour flavour) throws Exception {
        try (VeraPDFFoundry veraPDFFoundry = Foundries.defaultInstance();
             PDFAParser parser = veraPDFFoundry
                     .createParser(new ByteArrayInputStream(pdfBytes), flavour)) {

            return veraPDFFoundry
                    .createValidator(flavour, false)
                    .validate(parser);
        }
    }

    private @NotNull PDFAFlavour mapPdfVariantToVeraPDFFlavour(@NotNull PdfVariant pdfVariant) {
        return switch (pdfVariant) {
            case PDF_A_1B -> PDFAFlavour.PDFA_1_B;
            case PDF_A_2B -> PDFAFlavour.PDFA_2_B;
            case PDF_A_3B -> PDFAFlavour.PDFA_3_B;
            case PDF_A_4B -> PDFAFlavour.PDFA_4;
            case PDF_A_2U -> PDFAFlavour.PDFA_2_U;
            case PDF_A_3U -> PDFAFlavour.PDFA_3_U;
            case PDF_A_4U -> PDFAFlavour.PDFA_4;
            case PDF_UA_1 -> PDFAFlavour.PDFUA_1;
        };
    }

    private String injectDefaultCss(String html) {
        // Get default CSS from settings
        String defaultCss = new CssSettings(new SettingsService(null, null, null))
                .defaultValues()
                .getCss();

        // Parse HTML
        Document doc = Jsoup.parse(html);

        // Create and append style element to head
        Element head = doc.head();
        Element styleElement = new Element("style");
        styleElement.text(defaultCss);
        head.appendChild(styleElement);

        // Return modified HTML
        return doc.html();
    }
}
