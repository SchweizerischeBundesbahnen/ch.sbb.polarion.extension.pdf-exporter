package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
class TikaMimeTypeResolverTest {

    private TikaMimeTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TikaMimeTypeResolver();
    }

    @ParameterizedTest
    @CsvSource({
            "document.pdf, application/pdf",
            "image.png, image/png",
            "photo.jpg, image/jpeg",
            "page.html, text/html",
            "readme.txt, text/plain",
            "config.xml, application/xml",
            "data.json, application/json",
            "styles.css, text/css"
    })
    void testRunWithFileName(String fileName, String expectedMimeType) {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, fileName);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertEquals(expectedMimeType, mimeType.get());
    }

    @Test
    void testRunWithByteArrayPdfContent() {
        byte[] pdfContent = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, pdfContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertEquals("application/pdf", mimeType.get());
    }

    @Test
    void testRunWithByteArrayPngContent() {
        byte[] pngContent = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, pngContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertEquals("image/png", mimeType.get());
    }

    @Test
    void testRunWithByteArrayJpegContent() {
        byte[] jpegContent = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        };
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, jpegContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertEquals("image/jpeg", mimeType.get());
    }

    @Test
    void testRunWithByteArrayZipContent() {
        byte[] zipContent = new byte[]{
                0x50, 0x4B, 0x03, 0x04
        };
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, zipContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertEquals("application/zip", mimeType.get());
    }

    @Test
    void testRunWithUnknownFileNameReturnsEmptyOptional() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "unknown_file");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isEmpty();
    }

    @Test
    void testRunWithEmptyByteArrayReturnsEmptyOptional() {
        byte[] emptyContent = new byte[0];
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, emptyContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isEmpty();
    }

    @Test
    void testRunWithRandomByteArrayReturnsEmptyOptional() {
        byte[] randomContent = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, randomContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isEmpty();
    }


    @Test
    void testRunWithJavaScriptFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "script.js");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).containsIgnoringCase("javascript");
    }
}
