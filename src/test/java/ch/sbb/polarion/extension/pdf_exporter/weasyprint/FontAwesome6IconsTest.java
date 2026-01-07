package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Font Awesome 6 icons rendering in PDF export.
 * <p>
 * This test verifies that HTML content with Font Awesome 6 icons
 * (e.g., {@code <i class="fa-solid fa-chart-column">}) can be successfully
 * converted to PDF and renders correctly.
 * <p>
 * Font Awesome 6 support was added in version 12.0.0 to support Polarion 2512,
 * which uses FA6 icons instead of FA4.
 */
class FontAwesome6IconsTest extends BaseWeasyPrintTest {

    private static final String FA6_CSS = "fontAwesome6";
    private static final String FONT_FA6_SOLID = "fa-solid-900";
    private static final String FA6_FONT_BASE64_PLACEHOLDER = "{FA6_FONT_BASE64}";
    private static final String EXT_TTF = ".ttf";

    @Test
    @SneakyThrows
    void testExportFontAwesome6Icons() {
        String testName = "fontAwesome6Icons";
        String html = buildHtmlWithFa6Icons();

        // Verify placeholders are replaced
        assertThat(html)
                .as("Font placeholders should be replaced")
                .doesNotContain(FA6_FONT_BASE64_PLACEHOLDER)
                .doesNotContain(FONT_BASE64_REPLACE_PARAM);

        List<BufferedImage> pages = exportAndGetAsImages(testName, html);

        assertThat(pages)
                .as("PDF with Font Awesome 6 icons should be generated")
                .hasSize(1);

        // Compare with reference image
        BufferedImage resultImage = pages.getFirst();
        BufferedImage expectedImage = ImageIO.read(readPngResource(testName + PAGE_SUFFIX + 0));

        List<Point> diffPoints = MediaUtils.diffImages(expectedImage, resultImage);
        if (!diffPoints.isEmpty()) {
            MediaUtils.fillImagePoints(resultImage, diffPoints, Color.BLUE.getRGB());
            writeReportImage(testName + PAGE_SUFFIX + "0_diff", resultImage);
        }

        assertThat(diffPoints)
                .as("Generated PDF should match reference image (differences highlighted in blue in reports folder)")
                .isEmpty();
    }

    @SneakyThrows
    private String buildHtmlWithFa6Icons() {
        // Read HTML template
        String html = readHtmlResource("fontAwesome6Icons");

        // Build CSS with embedded FA6 font
        String css = buildFa6Css();

        // Wrap HTML with proper structure including CSS
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <style>
                    %s
                    </style>
                </head>
                %s
                </html>
                """.formatted(css, html.replace("<!DOCTYPE html>", "").replace("<html lang=\"en\">", "").replace("</html>", ""));
    }

    @SneakyThrows
    private String buildFa6Css() {
        // Read CSS template with FA6 font definitions
        String cssTemplate = readFa6CssTemplate();

        // Read and encode OpenSans font
        byte[] openSansBytes = readFontResource(FONT_REGULAR);
        String openSansBase64 = Base64.getEncoder().encodeToString(openSansBytes);

        // Read and encode FA6 font
        byte[] fa6FontBytes = readFontResourceTtf(FONT_FA6_SOLID);
        String fa6FontBase64 = Base64.getEncoder().encodeToString(fa6FontBytes);

        // Replace placeholders
        return cssTemplate
                .replace(FONT_BASE64_REPLACE_PARAM, openSansBase64)
                .replace(FA6_FONT_BASE64_PLACEHOLDER, fa6FontBase64);
    }

    @SneakyThrows
    @SuppressWarnings("ConstantConditions")
    private String readFa6CssTemplate() {
        try (InputStream is = getClass().getResourceAsStream(WEASYPRINT_TEST_CSS_RESOURCES_FOLDER + FA6_CSS + EXT_CSS)) {
            return StringUtils.readToString(is);
        }
    }

    @SneakyThrows
    private byte[] readFontResourceTtf(String resourceName) {
        try (InputStream is = getClass().getResourceAsStream(WEASYPRINT_TEST_FONT_RESOURCES_FOLDER + resourceName + EXT_TTF)) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        throw new IllegalArgumentException("Cannot load font " + resourceName);
    }
}
