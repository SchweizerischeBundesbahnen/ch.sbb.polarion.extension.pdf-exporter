package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class TikaMimeTypeResolverTest {

    private TikaMimeTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TikaMimeTypeResolver();
    }

    @Test
    void testRunWithPdfFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "document.pdf");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("application/pdf");
    }

    @Test
    void testRunWithImageFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "image.png");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("image/png");
    }

    @Test
    void testRunWithJpegFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "photo.jpg");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("image/jpeg");
    }

    @Test
    void testRunWithHtmlFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "page.html");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("text/html");
    }

    @Test
    void testRunWithTextFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "readme.txt");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("text/plain");
    }

    @Test
    void testRunWithXmlFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "config.xml");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("application/xml");
    }

    @Test
    void testRunWithJsonFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "data.json");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("application/json");
    }

    @Test
    void testRunWithByteArrayPdfContent() {
        byte[] pdfContent = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, pdfContent);
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("application/pdf");
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
        assertThat(mimeType.get()).isEqualTo("image/png");
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
        assertThat(mimeType.get()).isEqualTo("image/jpeg");
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
        assertThat(mimeType.get()).isEqualTo("application/zip");
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
    void testRunWithCssFileName() {
        Map<String, Object> input = Map.of(TikaMimeTypeResolver.PARAM_VALUE, "styles.css");
        Map<String, Object> result = resolver.run(input);

        assertThat(result).containsKey(TikaMimeTypeResolver.PARAM_RESULT);
        Optional<String> mimeType = (Optional<String>) result.get(TikaMimeTypeResolver.PARAM_RESULT);
        assertThat(mimeType).isPresent();
        assertThat(mimeType.get()).isEqualTo("text/css");
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
