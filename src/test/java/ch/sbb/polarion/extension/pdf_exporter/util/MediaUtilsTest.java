package ch.sbb.polarion.extension.pdf_exporter.util;

import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MediaUtilsTest {

    @Test
    void removeSvgUnsupportedFeatureHintTest() {
        // svg sample from polarion diagram v23.10
        assertEquals("<svg></svg>", MediaUtils.removeSvgUnsupportedFeatureHint("<svg><switch>" +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://www.diagrams.net/doc/faq/svg-export-text-problems\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Text is not SVG - cannot display</text>" +
                "<title>https://www.diagrams.net/doc/faq/svg-export-text-problems</title></a></switch>" +
                "</svg>"));

        // alternative hint from https://github.com/jgraph/drawio/issues/774 (also here is extra space before g)
        assertEquals("<svg></svg>", MediaUtils.removeSvgUnsupportedFeatureHint("<svg><switch> " +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://desk.draw.io/support/solutions/articles/16000042487\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Viewer does not support full SVG 1.1</text>" +
                "</a></switch>" +
                "</svg>"));

        // potential issue with another feature, main idea here is to cut down all requiredFeatures checks no matter which feature it is
        assertEquals("<svg></svg>", MediaUtils.removeSvgUnsupportedFeatureHint("<svg><switch>" +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Gradient\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://some.url\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Some warning</text>" +
                "</a></switch>" +
                "</svg>"));
    }

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
        assertNull(MediaUtils.guessMimeType("unknownExtensionNonsenseContent.unk", Bytes.toArray(List.of(0, 0, 0, 0, 0))));

        assertTrue(InMemoryAppender.anyMessageContains("Cannot get mime type for the resource: unknownExtensionEmptyContent.unk"));
        assertTrue(InMemoryAppender.anyMessageContains("Cannot get mime type for the resource: unknownExtensionNonsenseContent.unk"));
    }

}