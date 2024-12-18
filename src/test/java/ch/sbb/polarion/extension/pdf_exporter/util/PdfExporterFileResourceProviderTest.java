package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExporterFileResourceProviderTest {

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
        PdfExporterFileResourceProvider provider = mock(PdfExporterFileResourceProvider.class);
        when(provider.getResourceAsBytes("http://localhost/some-path/img.png", null)).thenReturn(imgBytes);
        when(provider.getResourceAsBase64String(any(), eq(null))).thenCallRealMethod();
        String result = provider.getResourceAsBase64String("http://localhost/some-path/img.png", null);
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes), result);
    }

}
