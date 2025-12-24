package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.VeraPdfValidationUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for PDF variant validation using veraPDF library.
 * Tests supported PDF variants by converting HTML with images to PDF and validating the result.
 * <p>
 * The following variants are excluded from parameterized validation tests:
 * <ul>
 *     <li>PDF_UA_2 - incomplete ISO 14289-2:2024 support in WeasyPrint 67.0</li>
 *     <li>PDF_A_4F - requires embedded files in the document by specification (ISO 19005-4:2020 clause 6.9),
 *         tested separately with embedded files in {@link #testPdfA4fWithEmbeddedFiles()} and
 *         {@link #testPdfA4fWithCoverPageAndEmbeddedFiles()}</li>
 * </ul>
 */
@ExtendWith({CurrentContextExtension.class, MockitoExtension.class})
@CurrentContextConfig("pdf-exporter")
class PdfVariantValidationTest extends BaseWeasyPrintTest {

    static {
        VeraGreenfieldFoundryProvider.initialise(); // Initialize veraPDF Greenfield foundry provider
    }

    @Mock
    private IModule module;

    @ParameterizedTest(name = "Test PDF conversion and validation for {0}")
    @EnumSource(value = PdfVariant.class, names = {"PDF_UA_2", "PDF_A_4F"}, mode = EnumSource.Mode.EXCLUDE)
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
        PDFAFlavour flavour = VeraPdfValidationUtils.mapPdfVariantToVeraPDFFlavour(pdfVariant);
        ValidationResult result = VeraPdfValidationUtils.validatePdf(pdfBytes, flavour);

        assertTrue(result.isCompliant(), String.format("PDF must be compliant with %s (VeraPDF flavour: %s). Failed rules: %s", pdfVariant, flavour, result.getTestAssertions()));
    }

    @ParameterizedTest(name = "Test PDF with replaced first page and validation for {0}")
    @EnumSource(value = PdfVariant.class, names = {"PDF_UA_2", "PDF_A_4F"}, mode = EnumSource.Mode.EXCLUDE)
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
        PDFAFlavour flavour = VeraPdfValidationUtils.mapPdfVariantToVeraPDFFlavour(pdfVariant);
        ValidationResult result = VeraPdfValidationUtils.validatePdf(pdfBytesWithCoverPage, flavour);

        assertTrue(result.isCompliant(), String.format("PDF with cover page must be compliant with %s (VeraPDF flavour: %s). Failed rules: %s", pdfVariant, flavour, result.getTestAssertions()));
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

    /**
     * Test PDF/UA-2 variant validation.
     * <p>
     * This test is expected to FAIL because WeasyPrint 67.0 has incomplete support for ISO 14289-2:2024 (PDF/UA-2).
     * The following issues are NOT fixed by post-processing and require WeasyPrint updates:
     * <ul>
     *     <li>Structure destinations required for all internal links (clause 7.18.3, test 1)</li>
     *     <li>PDF 2.0 namespace required for Document element (ISO 32005:2023)</li>
     *     <li>Document-Span restriction (ISO 32005:2023)</li>
     *     <li>ListNumbering attribute required for lists</li>
     * </ul>
     * <p>
     * The pdfuaid:rev fix (setting to "2024") is applied via post-processing.
     */
    @Test
    @SneakyThrows
    void testPdfUa2Validation() {
        // Read HTML resource with images
        String html = readHtmlResource("pdfVariantValidation");
        assertNotNull(html, "HTML resource should not be null");

        // Inject default CSS into HTML
        html = injectDefaultCss(html);

        // Convert HTML to PDF with PDF/UA-2 variant
        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(PdfVariant.PDF_UA_2)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");

        // Write PDF to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, "PDF_UA_2", pdfBytes);

        // Validate PDF using veraPDF
        PDFAFlavour flavour = PDFAFlavour.PDFUA_2;
        ValidationResult result = VeraPdfValidationUtils.validatePdf(pdfBytes, flavour);

        // Note: Full PDF/UA-2 compliance requires WeasyPrint updates for:
        // - Structure destinations for internal links (clause 8.8)
        // - PDF 2.0 namespace for Document element (clause 8.2.5.2)
        // - Document-Span restriction (ISO 32005:2023)
        // - ListNumbering attribute for lists (clause 8.2.5.25)
        // The pdfuaid:rev fix is applied via post-processing.

        // For now, we just verify that the PDF was generated and our post-processing was applied
        // (pdfuaid:rev error would appear in result.getTestAssertions() if not fixed)
        boolean hasPdfuaidRevError = result.getTestAssertions().stream()
                .anyMatch(a -> a.getMessage() != null && a.getMessage().contains("pdfuaid:rev"));
        assertFalse(hasPdfuaidRevError, "pdfuaid:rev should be fixed to '2024' by post-processing");
    }

    /**
     * Test PDF/A-4f variant with embedded files.
     * <p>
     * PDF/A-4f requires embedded files in the document by specification (ISO 19005-4:2020 clause 6.9).
     * This test validates that documents with attachments comply with the PDF/A-4f standard.
     */
    @Test
    @SneakyThrows
    void testPdfA4fWithEmbeddedFiles() {
        // Prepare attachment file
        List<Path> attachments = new ArrayList<>();
        URL resource = getClass().getResource("/test_img.png");
        assertNotNull(resource, "Test attachment file not found");
        File file = new File(resource.toURI());
        attachments.add(file.toPath());

        // Create document data with attachments
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("PDF/A-4f Test Document")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .attachmentFiles(attachments)
                .build();

        // Read and prepare HTML
        String html = readHtmlResource("pdfVariantValidation");
        assertNotNull(html, "HTML resource should not be null");
        html = injectDefaultCss(html);

        // Convert HTML to PDF with PDF/A-4f variant and attachments
        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(PdfVariant.PDF_A_4F)
                .build();

        byte[] pdfBytes = exportToPdf(html, options, documentData);
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");

        // Write PDF to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, "PDF_A_4F_with_attachments", pdfBytes);

        // Validate PDF using veraPDF
        PDFAFlavour flavour = PDFAFlavour.PDFA_4_F;
        ValidationResult result = VeraPdfValidationUtils.validatePdf(pdfBytes, flavour);

        assertTrue(result.isCompliant(), String.format(
                "PDF/A-4f with embedded files must be compliant (VeraPDF flavour: %s). Failed rules: %s",
                flavour, result.getTestAssertions()));
    }

    /**
     * Test PDF/A-4f variant with cover page replacement and embedded files.
     * <p>
     * This test validates that merged PDF documents with attachments comply with the PDF/A-4f standard.
     */
    @Test
    @SneakyThrows
    void testPdfA4fWithCoverPageAndEmbeddedFiles() {
        // Prepare attachment file
        List<Path> attachments = new ArrayList<>();
        URL resource = getClass().getResource("/test_img.png");
        assertNotNull(resource, "Test attachment file not found");
        File file = new File(resource.toURI());
        attachments.add(file.toPath());

        // Create document data with attachments
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("PDF/A-4f Test Document with Cover Page")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .attachmentFiles(attachments)
                .build();

        // Read and prepare HTML for cover page and main content
        String coverPageHtml = readHtmlResource("pdfVariantValidationCoverPage");
        assertNotNull(coverPageHtml, "Cover page HTML resource should not be null");
        coverPageHtml = injectDefaultCss(coverPageHtml);

        String contentHtml = readHtmlResource("pdfVariantValidation");
        assertNotNull(contentHtml, "Content HTML resource should not be null");
        contentHtml = injectDefaultCss(contentHtml);

        // Convert HTML to PDF with PDF/A-4f variant
        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(PdfVariant.PDF_A_4F)
                .build();

        // Generate cover page PDF (without attachments)
        byte[] coverPagePdfBytes = exportToPdf(coverPageHtml, options);
        assertNotNull(coverPagePdfBytes, "Cover page PDF bytes should not be null");

        // Generate content PDF with attachments
        byte[] contentPdfBytes = exportToPdf(contentHtml, options, documentData);
        assertNotNull(contentPdfBytes, "Content PDF bytes should not be null");

        // Replace first page of content PDF with cover page
        byte[] pdfBytesWithCoverPage = MediaUtils.overwriteFirstPageWithTitle(
                contentPdfBytes, coverPagePdfBytes, PdfVariant.PDF_A_4F);
        assertNotNull(pdfBytesWithCoverPage, "PDF with cover page should not be null");
        assertTrue(pdfBytesWithCoverPage.length > 0, "PDF with cover page should not be empty");

        // Write PDF to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, "PDF_A_4F_with_cover_and_attachments", pdfBytesWithCoverPage);

        // Validate PDF using veraPDF
        PDFAFlavour flavour = PDFAFlavour.PDFA_4_F;
        ValidationResult result = VeraPdfValidationUtils.validatePdf(pdfBytesWithCoverPage, flavour);

        assertTrue(result.isCompliant(), String.format(
                "PDF/A-4f with cover page and embedded files must be compliant (VeraPDF flavour: %s). Failed rules: %s",
                flavour, result.getTestAssertions()));
    }
}
