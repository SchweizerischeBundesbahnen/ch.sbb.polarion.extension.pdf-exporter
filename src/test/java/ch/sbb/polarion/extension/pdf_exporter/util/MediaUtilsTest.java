package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import static ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils.THUMBNAIL_PARAMETER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, BundleJarsPrioritizingRunnableMockExtension.class, PdfExporterExtensionConfigurationExtension.class})
class MediaUtilsTest {

    @Test
    void dataUrlTest() {
        assertFalse(MediaUtils.isDataUrl(null));
        assertFalse(MediaUtils.isDataUrl(""));
        assertFalse(MediaUtils.isDataUrl("some data"));
        assertFalse(MediaUtils.isDataUrl("data : 123"));
        assertFalse(MediaUtils.isDataUrl("DATA:123"));
        assertFalse(MediaUtils.isDataUrl(" data:123"));
        assertTrue(MediaUtils.isDataUrl("data:123"));
        assertTrue(MediaUtils.isDataUrl("data:   123"));
    }

    @Test
    @SneakyThrows
    void mimeTypeRecognitionTest() {
        byte[] emptyArray = new byte[0];
        assertEquals("image/png", MediaUtils.guessMimeType("https://example.com/imgs/img.png", emptyArray));
        assertEquals("image/png", MediaUtils.guessMimeType("https://example.com/imgs/img.png?someParam=123", emptyArray));
        assertEquals("image/jpeg", MediaUtils.guessMimeType("https://example.com/imgs/img.jpeg", emptyArray));
        assertEquals("image/jpeg", MediaUtils.guessMimeType("https://example.com/imgs/img.jpg?someParam=123", emptyArray));
        assertEquals("image/svg+xml", MediaUtils.guessMimeType("https://example.com/imgs/img.svg?someParam=123", emptyArray));
        assertEquals("image/bmp", MediaUtils.guessMimeType("/some/relative/url/img.BMP?someParam=123", emptyArray));
        assertEquals("image/gif", MediaUtils.guessMimeType("img.gif?someParam=123", emptyArray));
        assertEquals("image/tiff", MediaUtils.guessMimeType("img.tiff", emptyArray));
        assertEquals("image/x-icon", MediaUtils.guessMimeType("/img.cur?ver=1.5f", emptyArray));
        assertEquals("application/font-ttf", MediaUtils.guessMimeType("example.com/fonts/someTrueType.ttf", emptyArray));
        assertEquals("application/font-woff", MediaUtils.guessMimeType("https://example.com/fonts/someWoffFont.woff", emptyArray));
        assertEquals("application/font-woff", MediaUtils.guessMimeType("someWoffFont.WOFF", emptyArray));
        assertEquals("image/png", MediaUtils.guessMimeType("https://example.com/imgs/no_extension", IOUtils.resourceToByteArray("/test_img.png")));
        assertEquals("text/plain", MediaUtils.guessMimeType("noExtension", "text".getBytes()));

        assertNull(MediaUtils.guessMimeType("unknownExtensionEmptyContent.unk", emptyArray));
        assertNull(MediaUtils.guessMimeType("unknownExtensionNonsenseContent.unk", new byte[] {0, 0, 0, 0, 0}));

        assertTrue(InMemoryAppender.anyMessageContains("Cannot get mime type for the resource: unknownExtensionEmptyContent.unk"));
        assertTrue(InMemoryAppender.anyMessageContains("Cannot get mime type for the resource: unknownExtensionNonsenseContent.unk"));
    }

    @Test
    void sameImages_WithIdenticalImages_ReturnsTrue() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(image2, Color.WHITE);

        assertTrue(MediaUtils.sameImages(image1, image2));
    }

    @Test
    void sameImages_WithDifferentImages_ReturnsFalse() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(image2, Color.BLACK);

        assertFalse(MediaUtils.sameImages(image1, image2));
    }

    @Test
    void diffImages_WithIdenticalImages_ReturnsEmptyList() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(image2, Color.WHITE);

        assertTrue(MediaUtils.diffImages(image1, image2).isEmpty());
    }

    @Test
    void diffImages_WithDifferentImages_ReturnsDifferentPoints() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(image2, Color.BLACK);

        assertEquals(100 * 100, MediaUtils.diffImages(image1, image2).size()); // All pixels should be different
    }

    @Test
    void diffImages_WithDifferentSizes_ReturnsBorderPoints() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage smallerImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(smallerImage, Color.WHITE);

        List<Point> differences = MediaUtils.diffImages(image1, smallerImage);
        // Expected points = top edge + bottom edge + left edge (without corners) + right edge (without corners)
        int expectedPoints = 50 + 50 + 48 + 48;
        assertEquals(expectedPoints, differences.size());

        // Verify border points
        assertTrue(differences.contains(new Point(0, 0))); // Top-left corner
        assertTrue(differences.contains(new Point(49, 0))); // Top-right corner
        assertTrue(differences.contains(new Point(0, 49))); // Bottom-left corner
        assertTrue(differences.contains(new Point(49, 49))); // Bottom-right corner
    }

    @Test
    void diffImages_WithSinglePixelDifference_ReturnsOnePoint() {
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        fillImageWithColor(image1, Color.WHITE);
        fillImageWithColor(image2, Color.WHITE);
        image2.setRGB(50, 50, Color.BLACK.getRGB());

        List<Point> differences = MediaUtils.diffImages(image1, image2);
        assertEquals(1, differences.size());
        assertEquals(new Point(50, 50), differences.get(0));
    }

    @ParameterizedTest
    @CsvSource({
            "'http://localhost/polarion/module-attachment/elibrary/Specification/Administration%20Specification/sample_diagram.vsdx?revision=2&thumbnail=true', 'http://localhost/polarion/module-attachment/elibrary/Specification/Administration%20Specification/sample_diagram.vsdx?revision=2'",
            "'http://localhost/polarion/attachment.vsdx?thumbnail=true&revision=2', 'http://localhost/polarion/attachment.vsdx?revision=2'",
            "'http://localhost/polarion/attachment.vsdx?revision=2&view=true', 'http://localhost/polarion/attachment.vsdx?revision=2&view=true'",
            "'http://localhost/polarion/attachment.vsdx?thumbnail=true', 'http://localhost/polarion/attachment.vsdx'",
            "'http://localhost/polarion/attachment.vsdx?name=test%20file&thumbnail=true&revision=2', 'http://localhost/polarion/attachment.vsdx?name=test%20file&revision=2'",
            "'http://localhost/polarion/attachment.vsdx?revision=2&thumbnail=true&view=true', 'http://localhost/polarion/attachment.vsdx?revision=2&view=true'",
            "'http://localhost/polarion/attachment.vsdx?thumbnail=false&revision=2', 'http://localhost/polarion/attachment.vsdx?revision=2'",
            "'http://localhost/polarion/attachment.vsdx?revision=4&amp;thumbnail=true', 'http://localhost/polarion/attachment.vsdx?revision=4'",
            ", ",
            "'', ''",
            "'not-a-valid-url?thumbnail=true', 'not-a-valid-url'",
            "'/polarion/icons/project/elibrary/star_16x16 .png', '/polarion/icons/project/elibrary/star_16x16 .png'",
            "'/polarion/icons/project/elibrary/star_16x16 .png?thumbnail=true', '/polarion/icons/project/elibrary/star_16x16 .png'"
    })
    void removeQueryParameterTest(String input, String expected) {
        assertEquals(expected, MediaUtils.removeQueryParameter(input, THUMBNAIL_PARAMETER));
    }

    @Test
    void isRenderableImageUrlTest() {
        when(PdfExporterExtensionConfiguration.getInstance().getRenderableImageExtensions())
                .thenReturn(Set.of("png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "avif", "ico", "cur", "tif", "tiff", "vsdx"));
        // Image formats — renderable (thumbnail stripped)
        assertTrue(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/photo.png"));
        assertTrue(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/tw.svg?revision=123&view=true"));
        assertTrue(MediaUtils.isRenderableImageUrl("/polarion/icons/default/enums/req_status_draft.gif?buildId=123"));
        assertTrue(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/photo.avif"));
        // Convertible diagram formats — renderable
        assertTrue(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/diagram.vsdx?revision=2&thumbnail=true"));
        // Non-renderable attachments — keep thumbnail (icon preview)
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/Book1.xlsx?thumbnail=true"));
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/doc.docx?thumbnail=true"));
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/file.pdf?thumbnail=true"));
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/archive.zip?thumbnail=true"));
        // Short (2-char) extensions are handled too — would be missed by a {3,4} regex
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/archive.7z?thumbnail=true"));
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/archive.tar.gz?thumbnail=true"));
        // Unknown formats default to non-renderable (thumbnail kept) to avoid inlining broken binaries into <img>
        assertFalse(MediaUtils.isRenderableImageUrl("http://localhost/polarion/wi-attachment/project/WI-1/data.bin?thumbnail=true"));
        // Edge cases
        assertFalse(MediaUtils.isRenderableImageUrl(null));
        assertFalse(MediaUtils.isRenderableImageUrl(""));
    }

    @Test
    void getResourceExtensionTest() {
        assertEquals("png", MediaUtils.getResourceExtension("http://localhost/img.png"));
        assertEquals("png", MediaUtils.getResourceExtension("http://localhost/img.PNG?thumbnail=true"));
        assertEquals("svg", MediaUtils.getResourceExtension("/path/tw.svg?revision=123&view=true"));
        assertEquals("svg", MediaUtils.getResourceExtension("/path/tw.svg#fragment"));
        // 2-char extensions
        assertEquals("gz", MediaUtils.getResourceExtension("/path/archive.tar.gz"));
        assertEquals("7z", MediaUtils.getResourceExtension("/path/archive.7z?x=1"));
        // No extension / dots only in the path, not in the file name
        assertEquals("", MediaUtils.getResourceExtension("http://example.com/imgs/no_extension"));
        assertEquals("", MediaUtils.getResourceExtension("/path/file."));
        assertEquals("", MediaUtils.getResourceExtension(null));
        assertEquals("", MediaUtils.getResourceExtension(""));
    }

    private void fillImageWithColor(BufferedImage image, Color color) {
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.dispose();
    }

}
