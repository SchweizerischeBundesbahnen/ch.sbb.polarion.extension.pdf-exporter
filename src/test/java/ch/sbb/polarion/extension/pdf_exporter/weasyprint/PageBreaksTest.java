package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The page breaks of a landscape document turn the page: the page above the first is landscape, the one above the
 * second portrait, and the last page keeps the orientation of the document.
 */
class PageBreaksTest extends BasePdfConverterTest {

    public static final String RESOURCE_NAME = "pageBreaks";

    @Test
    void testPageBreaks() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.LANDSCAPE)
                .paperSize(PaperSize.A4)
                .build();

        byte[] pdf = exportLiveDoc("Page breaks", readHtmlResource(RESOURCE_NAME), params);

        List<BufferedImage> images = getAllPagesAsImagesAndLogAsReports(RESOURCE_NAME, pdf);
        assertThat(images).size().isEqualTo(3);
        assertThat(isLandscape(images.get(0))).isTrue();
        assertThat(isLandscape(images.get(1))).isFalse();
        assertThat(isLandscape(images.get(2))).isTrue();
    }

    private boolean isLandscape(BufferedImage image) {
        return image.getWidth() > image.getHeight();
    }
}
