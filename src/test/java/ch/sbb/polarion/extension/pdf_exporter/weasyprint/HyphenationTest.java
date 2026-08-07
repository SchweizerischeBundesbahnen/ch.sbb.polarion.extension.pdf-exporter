package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the document-language feature (#983): the language injected into the {@code <html lang>}
 * attribute drives WeasyPrint's hyphenation.
 * <p>
 * Two fixtures containing long words and narrow columns/table cells are exported through the real WeasyPrint service -
 * {@code hyphenationDe.html} declares {@code lang='de'}, {@code hyphenationEn.html} declares {@code lang='en'}. With
 * {@code hyphens: auto}, WeasyPrint hyphenates at the syllable boundaries of the declared language, so the rendered
 * pages must match language-specific reference images pixel-for-pixel ({@link MediaUtils#diffImages}). This proves the
 * injected {@code lang} value actually reaches WeasyPrint and is honored per language.
 */
class HyphenationTest extends BaseWeasyPrintTest {

    @Test
    void germanHyphenationMatchesReference() {
        assertMatchesReference("hyphenationDe");
    }

    @Test
    void englishHyphenationMatchesReference() {
        assertMatchesReference("hyphenationEn");
    }

    @SneakyThrows
    private void assertMatchesReference(String testName) {
        List<BufferedImage> pages = exportAndGetAsImages(testName, readHtmlResource(testName));

        assertThat(pages)
                .as("Hyphenation export should produce a single page")
                .hasSize(1);

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
}
