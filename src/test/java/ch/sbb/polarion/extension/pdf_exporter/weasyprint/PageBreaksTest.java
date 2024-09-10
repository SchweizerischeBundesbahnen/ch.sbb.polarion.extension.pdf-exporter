package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PageBreaksTest extends BaseWeasyPrintTest {

    public static final String RESOURCE_NAME = "pageBreaks";

    @Test
    @SneakyThrows
    void testPageBreaks() {
        List<BufferedImage> images = exportAndGetAsImages(RESOURCE_NAME);
        assertThat(images).size().isEqualTo(3);
        assertThat(isLandscape(images.get(0))).isTrue();
        assertThat(isLandscape(images.get(1))).isFalse();
        assertThat(isLandscape(images.get(2))).isTrue();
    }

    private boolean isLandscape(BufferedImage image) {
        return image.getWidth() > image.getHeight();
    }
}
