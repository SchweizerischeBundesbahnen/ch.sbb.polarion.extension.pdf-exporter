package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExporterFileResourceProviderTest {
    @Mock
    PdfExporterFileResourceProvider provider;

    @Test
    void processPossibleSvgImageTest() {
        byte[] basicString = "basic".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(basicString, new PdfExporterFileResourceProvider(new ArrayList<>()).processPossibleSvgImage(basicString));
    }

    @Test
    @SneakyThrows
    void replaceImagesAsBase64EncodedTest() {
        byte[] imgBytes;
        try (InputStream is = this.getClass().getResourceAsStream("/test_img.png")) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }

        when(provider.getResourceAsBytes("http://localhost/some-path/img.png", null)).thenReturn(imgBytes);
        when(provider.getResourceAsBase64String(any(), eq(null))).thenCallRealMethod();
        String result = provider.getResourceAsBase64String("http://localhost/some-path/img.png", null);
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes), result);
    }

    @Test
    void isMediaTypeMismatch_MatchingMimeTypes() {
        String resource = "image.png";
        byte[] content = new byte[0];

        try (MockedStatic<MediaUtils> mockedMediaUtils = Mockito.mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("image/png");
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("image/png");

            boolean result = provider.isMediaTypeMismatch(resource, content);
            assertFalse(result);
        }
    }

    @Test
    @SneakyThrows
    void getDefaultContentEmptyForNonImage() {
        String resource = "attachment.txt";
        try (MockedStatic<MediaUtils> mockedMediaUtils = Mockito.mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getImageFormat(resource))
                    .thenReturn("");

            byte[] content = provider.getDefaultContent(resource);
            assertNull(content);
        }
    }

    @Test
    void getWorkItemIdFromAttachmentUrlValidUrl() {
        String url = "http://localhost/polarion/wi-attachment/elibrary/EL-14852/attachment.png";
        String result = new PdfExporterFileResourceProvider(new ArrayList<>()).getWorkItemIdFromAttachmentUrl(url);
        assertEquals("EL-14852", result);
    }

    @Test
    void getWorkItemIdFromAttachmentUrl_InvalidUrl() {
        String url = "http://example.com/invalid/url";
        String result = provider.getWorkItemIdFromAttachmentUrl(url);
        assertNull(result);
    }
}
