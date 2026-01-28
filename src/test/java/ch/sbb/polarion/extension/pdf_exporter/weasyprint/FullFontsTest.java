package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the fullFonts parameter.
 * <p>
 * When fullFonts=true, fonts are embedded in their entirety without subsetting.
 * This helps avoid font subsetting errors (e.g., invalid OS/2 Unicode range bits)
 * but results in larger PDF files.
 * <p>
 * Tests cover 4 scenarios:
 * <ul>
 *     <li>Bad font + fullFonts=false → should fail (500 error from WeasyPrint)</li>
 *     <li>Bad font + fullFonts=true → should succeed</li>
 *     <li>Good font + fullFonts=false → should succeed</li>
 *     <li>Good font + fullFonts=true → should succeed</li>
 * </ul>
 */
class FullFontsTest extends BaseWeasyPrintTest {

    private static final String BAD_FONT_HTML = "fullFontsBadFont";
    private static final String GOOD_FONT_HTML = "fullFontsGoodFont";

    /**
     * Tests that HTML with a font containing invalid OS/2 Unicode range bits fails
     * without fullFonts=true.
     * <p>
     * This test verifies that font subsetting fails for fonts with invalid Unicode
     * range bits (bit 123 > max 122) when fullFonts is not enabled.
     */
    @Test
    @SneakyThrows
    void testBadFontFailsWithoutFullFonts() {
        String html = readHtmlResource(BAD_FONT_HTML);

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .fullFonts(false)
                .build();

        assertThatThrownBy(() -> exportToPdf(html, options))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("500")
                .hasMessageContaining("expected 0 <= int <= 122");
    }

    /**
     * Tests that HTML with a font containing invalid OS/2 Unicode range bits succeeds
     * with fullFonts=true.
     * <p>
     * This test verifies that disabling font subsetting (fullFonts=true) allows
     * conversion of fonts with invalid Unicode range bits that would otherwise fail
     * during subsetting.
     */
    @Test
    @SneakyThrows
    void testBadFontSucceedsWithFullFonts() {
        String html = readHtmlResource(BAD_FONT_HTML);

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .fullFonts(true)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);

        writeReportPdf(getCurrentMethodName(), "bad_font_full_fonts", pdfBytes);

        assertThat(pdfBytes)
                .as("PDF should be generated with fullFonts=true for bad font")
                .isNotNull()
                .isNotEmpty();

        assertThat(new String(pdfBytes, 0, 4))
                .as("Generated file should be a valid PDF")
                .isEqualTo("%PDF");
    }

    /**
     * Tests that HTML with a valid font succeeds with default font subsetting
     * (fullFonts=false).
     */
    @Test
    @SneakyThrows
    void testGoodFontSucceedsWithSubsetting() {
        String html = readHtmlResource(GOOD_FONT_HTML);

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .fullFonts(false)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);

        writeReportPdf(getCurrentMethodName(), "good_font_subsetting", pdfBytes);

        assertThat(pdfBytes)
                .as("PDF should be generated with font subsetting for good font")
                .isNotNull()
                .isNotEmpty();

        assertThat(new String(pdfBytes, 0, 4))
                .as("Generated file should be a valid PDF")
                .isEqualTo("%PDF");
    }

    /**
     * Tests that HTML with a valid font succeeds with fullFonts=true (no subsetting).
     */
    @Test
    @SneakyThrows
    void testGoodFontSucceedsWithFullFonts() {
        String html = readHtmlResource(GOOD_FONT_HTML);

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .fullFonts(true)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);

        writeReportPdf(getCurrentMethodName(), "good_font_full_fonts", pdfBytes);

        assertThat(pdfBytes)
                .as("PDF should be generated with fullFonts=true for good font")
                .isNotNull()
                .isNotEmpty();

        assertThat(new String(pdfBytes, 0, 4))
                .as("Generated file should be a valid PDF")
                .isEqualTo("%PDF");
    }

    /**
     * Tests that PDF generated with fullFonts=true is larger than PDF generated
     * with fullFonts=false (default), confirming that full fonts are embedded
     * without subsetting.
     */
    @Test
    @SneakyThrows
    void testFullFontsProducesLargerPdf() {
        String html = readHtmlResource(GOOD_FONT_HTML);

        // Generate PDF with fullFonts=false (default - font subsetting enabled)
        WeasyPrintOptions optionsWithSubsetting = WeasyPrintOptions.builder()
                .fullFonts(false)
                .build();
        byte[] pdfWithSubsetting = exportToPdf(html, optionsWithSubsetting);

        // Generate PDF with fullFonts=true (full fonts, no subsetting)
        WeasyPrintOptions optionsWithFullFonts = WeasyPrintOptions.builder()
                .fullFonts(true)
                .build();
        byte[] pdfWithFullFonts = exportToPdf(html, optionsWithFullFonts);

        // Write PDFs to reports folder for manual inspection
        String testName = getCurrentMethodName();
        writeReportPdf(testName, "with_subsetting", pdfWithSubsetting);
        writeReportPdf(testName, "with_full_fonts", pdfWithFullFonts);

        // Verify both PDFs were generated successfully
        assertThat(pdfWithSubsetting)
                .as("PDF with font subsetting should be generated")
                .isNotNull()
                .isNotEmpty();

        assertThat(pdfWithFullFonts)
                .as("PDF with full fonts should be generated")
                .isNotNull()
                .isNotEmpty();

        // PDF with full fonts should be larger because fonts are not subsetted
        assertThat(pdfWithFullFonts.length)
                .as("PDF with full fonts should be larger than PDF with font subsetting")
                .isGreaterThan(pdfWithSubsetting.length);
    }
}
