package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the fullFonts parameter.
 * <p>
 * When fullFonts=true, fonts are embedded in their entirety without subsetting: full glyph coverage and a PDF that
 * is easier to edit downstream, at the cost of a larger file.
 * <p>
 * The option used to be a workaround for fonts that broke subsetting (e.g. invalid OS/2 Unicode range bits). That
 * justification is gone: since WeasyPrint 69 subsetting no longer fails hard but logs and keeps the full font, so
 * such a font converts fine with fullFonts=false as well. The risk has in fact inverted - skipping subsetting is
 * what can fail on a damaged font - which is why the "bad font" case below is asserted for both values.
 * <p>
 * Tests cover the following scenarios:
 * <ul>
 *     <li>Bad font + fullFonts=true → should succeed</li>
 *     <li>Bad font + fullFonts=false → should succeed as well (guards the claim above against engine bumps)</li>
 *     <li>Good font + fullFonts=false → should succeed</li>
 *     <li>Good font + fullFonts=true → should succeed</li>
 *     <li>fullFonts=true produces a larger PDF than fullFonts=false</li>
 * </ul>
 */
class FullFontsTest extends BaseWeasyPrintTest {

    private static final String BAD_FONT_HTML = "fullFontsBadFont";
    private static final String GOOD_FONT_HTML = "fullFontsGoodFont";

    /**
     * Tests that HTML with a font containing invalid OS/2 Unicode range bits succeeds
     * with fullFonts=true.
     * <p>
     * Such a font used to fail during subsetting, which is no longer the case (see the test below), so this only
     * asserts that skipping subsetting still copes with it.
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
     * Tests that a font which used to break subsetting converts fine with subsetting enabled as well.
     * <p>
     * This is what made the original justification of fullFonts obsolete: since WeasyPrint 69 a font that cannot be
     * subset no longer fails the conversion, it is kept whole instead. Should a future engine bring the hard failure
     * back, this test turns red and the option's description has to be revisited.
     */
    @Test
    @SneakyThrows
    void testBadFontSucceedsWithSubsetting() {
        String html = readHtmlResource(BAD_FONT_HTML);

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .fullFonts(false)
                .build();

        byte[] pdfBytes = exportToPdf(html, options);

        writeReportPdf(getCurrentMethodName(), "bad_font_subsetting", pdfBytes);

        assertThat(pdfBytes)
                .as("PDF should be generated with fullFonts=false for bad font, the engine keeps the font whole")
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

        // PDF with full fonts should be larger because fonts are not subsetted
        assertThat(pdfWithFullFonts)
                .as("PDF with full fonts should be generated")
                .isNotNull()
                .isNotEmpty()
                .as("PDF with full fonts should be larger than PDF with font subsetting")
                .hasSizeGreaterThan(pdfWithSubsetting.length);
    }
}
