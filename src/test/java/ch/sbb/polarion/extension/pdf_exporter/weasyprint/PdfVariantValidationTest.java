package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
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
 */
@ExtendWith({CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class PdfVariantValidationTest extends BaseWeasyPrintTest {

    static {
        VeraGreenfieldFoundryProvider.initialise(); // Initialize veraPDF Greenfield foundry provider
    }

    @ParameterizedTest(name = "Test PDF conversion and validation for {0}")
    @EnumSource(PdfVariant.class)
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

    @ParameterizedTest(name = "Test PDF with replaced first page and validation for {0}")
    @EnumSource(PdfVariant.class)
    @SneakyThrows
    void testPdfVariantConversionWithCoverPageAndValidation(PdfVariant pdfVariant) {
        // Read HTML resources for cover page and main content
        String coverPageHtml = readHtmlResource("pdfVariantValidationCoverPage");
        assertNotNull(coverPageHtml, "Cover page HTML resource should not be null");

        String contentHtml = readHtmlResource("pdfVariantValidation");
        assertNotNull(contentHtml, "Content HTML resource should not be null");

        // Inject default CSS into both HTML documents
        coverPageHtml = injectDefaultCss(coverPageHtml);
        contentHtml = injectDefaultCss(contentHtml);

        // Convert both HTML documents to PDF with the specified PDF variant
        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(pdfVariant)
                .build();

        byte[] coverPagePdfBytes = exportToPdf(coverPageHtml, options);
        assertNotNull(coverPagePdfBytes, "Cover page PDF bytes should not be null");
        assertTrue(coverPagePdfBytes.length > 0, "Cover page PDF should not be empty");

        byte[] contentPdfBytes = exportToPdf(contentHtml, options);
        assertNotNull(contentPdfBytes, "Content PDF bytes should not be null");
        assertTrue(contentPdfBytes.length > 0, "Content PDF should not be empty");

        // Replace first page of content PDF with cover page
        // Note: MediaUtils.overwriteFirstPageWithTitle() automatically applies PDF/A post-processing based on pdfVariant
        byte[] pdfBytesWithCoverPage = MediaUtils.overwriteFirstPageWithTitle(contentPdfBytes, coverPagePdfBytes, pdfVariant);
        assertNotNull(pdfBytesWithCoverPage, "PDF with cover page should not be null");
        assertTrue(pdfBytesWithCoverPage.length > 0, "PDF with cover page should not be empty");

        // Write PDF to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, pdfVariant.name() + "_with_cover", pdfBytesWithCoverPage);

        // Validate PDF using veraPDF
        PDFAFlavour flavour = mapPdfVariantToVeraPDFFlavour(pdfVariant);
        ValidationResult result = validatePdf(pdfBytesWithCoverPage, flavour);

        assertTrue(result.isCompliant(), String.format("PDF with cover page must be compliant with %s (VeraPDF flavour: %s). Failed rules: %s", pdfVariant, flavour, result.getTestAssertions()));
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
            case PDF_A_4B, PDF_A_4U -> PDFAFlavour.PDFA_4;
            case PDF_A_2U -> PDFAFlavour.PDFA_2_U;
            case PDF_A_3U -> PDFAFlavour.PDFA_3_U;
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
