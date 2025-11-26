package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.tracker.internal.url.GenericUrlResolver;
import com.polarion.alm.tracker.internal.url.IAttachmentUrlResolver;
import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.alm.tracker.internal.url.ParentUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
import com.polarion.alm.tracker.internal.url.WorkItemAttachmentUrlResolver;
import com.polarion.core.util.StreamUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({
        MockitoExtension.class,
        TransactionalExecutorExtension.class,
        PlatformContextMockExtension.class
})
class PdfExporterFileResourceProviderTest {
    @Mock
    private IUrlResolver resolverMock;

    private PdfExporterFileResourceProvider resourceProvider;

    private static final String TEST_RESOURCE = "test-resource";
    private static final byte[] TEST_CONTENT = "test-content".getBytes();

    @BeforeEach
    void setUp() {
        resourceProvider = new PdfExporterFileResourceProvider(List.of(resolverMock));
    }

    @Test
    @SneakyThrows
    void replaceImagesAsBase64EncodedTest() {
        byte[] imgBytes;
        try (InputStream is = this.getClass().getResourceAsStream("/test_img.png")) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }

        PdfExporterFileResourceProvider resourceProviderMock = mock(PdfExporterFileResourceProvider.class);
        when(resourceProviderMock.getResourceAsBytes("http://localhost/some-path/img.png")).thenReturn(imgBytes);
        when(resourceProviderMock.getResourceAsBase64String(any())).thenCallRealMethod();
        String result = resourceProviderMock.getResourceAsBase64String("http://localhost/some-path/img.png");
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes), result);
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesSuccess() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(true);
        when(resolverMock.resolve(TEST_RESOURCE)).thenReturn(new ByteArrayInputStream(TEST_CONTENT));
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE);
        assertArrayEquals(TEST_CONTENT, result);
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesNoResolverFound() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(false);
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesResolverReturnsNull() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(true);
        when(resolverMock.resolve(TEST_RESOURCE)).thenReturn(null);
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void getResourceAsBytesExceptionHandling() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenThrow(new RuntimeException("Test Exception"));

        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE);

        assertArrayEquals(new byte[0], result);
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplSuccessfully() {
        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class)) {
            String resource = "valid/resource/url";
            byte[] expectedBytes = "expectedBytes".getBytes();
            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(expectedBytes);

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(expectedBytes));

            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(expectedBytes, result);
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplEmptyResult() {
        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class)) {
            String resource = "valid/resource/url";
            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(new byte[0]);

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(new byte[0]));

            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(new byte[0], result);
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplWorkItemAttachmentWithMismatch() {
        String resource = "/polarion/wi-attachment/project/WI-123/image.png";
        byte[] htmlContent = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>Login</body></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class);
             MockedStatic<WorkItemAttachmentUrlResolver> workItemMockedStatic = mockStatic(WorkItemAttachmentUrlResolver.class);
             MockedStatic<MediaUtils> mediaUtilsMockedStatic = mockStatic(MediaUtils.class)) {

            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(htmlContent);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource)).thenReturn(true);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isSvg(resource)).thenReturn(false);

            mediaUtilsMockedStatic.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, htmlContent))
                    .thenReturn(MediaType.APPLICATION_XHTML_XML);
            mediaUtilsMockedStatic.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("image/png");
            mediaUtilsMockedStatic.when(() -> MediaUtils.getImageFormat(resource)).thenReturn("");

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(htmlContent));

            ExportContext.clear();
            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(new byte[0], result);
            assertTrue(ExportContext.getWorkItemIDsWithMissingAttachment().contains("WI-123"));
            ExportContext.clear();
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplWorkItemAttachmentSvgSkipsMismatchCheck() {
        String resource = "/polarion/wi-attachment/project/WI-123/image.svg";
        byte[] svgContent = "<svg></svg>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class);
             MockedStatic<WorkItemAttachmentUrlResolver> workItemMockedStatic = mockStatic(WorkItemAttachmentUrlResolver.class)) {

            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(svgContent);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource)).thenReturn(true);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isSvg(resource)).thenReturn(true);

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(svgContent));

            ExportContext.clear();
            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(svgContent, result);
            assertTrue(ExportContext.getWorkItemIDsWithMissingAttachment().isEmpty());
            ExportContext.clear();
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplWorkItemAttachmentMatchingTypes() {
        String resource = "/polarion/wi-attachment/project/WI-123/image.png";
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class);
             MockedStatic<WorkItemAttachmentUrlResolver> workItemMockedStatic = mockStatic(WorkItemAttachmentUrlResolver.class);
             MockedStatic<MediaUtils> mediaUtilsMockedStatic = mockStatic(MediaUtils.class)) {

            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(pngContent);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource)).thenReturn(true);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isSvg(resource)).thenReturn(false);

            mediaUtilsMockedStatic.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, pngContent))
                    .thenReturn("image/png");
            mediaUtilsMockedStatic.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("image/png");

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(pngContent));

            ExportContext.clear();
            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(pngContent, result);
            assertTrue(ExportContext.getWorkItemIDsWithMissingAttachment().isEmpty());
            ExportContext.clear();
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplNotWorkItemAttachment() {
        String resource = "/polarion/some/other/resource.png";
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class);
             MockedStatic<WorkItemAttachmentUrlResolver> workItemMockedStatic = mockStatic(WorkItemAttachmentUrlResolver.class)) {

            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class))).thenReturn(pngContent);
            workItemMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource)).thenReturn(false);

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(pngContent));

            ExportContext.clear();
            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(resolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(pngContent, result);
            assertTrue(ExportContext.getWorkItemIDsWithMissingAttachment().isEmpty());
            ExportContext.clear();
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplMultipleResolversFirstReturnsEmpty() {
        String resource = "test/resource.png";
        byte[] expectedContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

        IUrlResolver firstResolver = mock(IUrlResolver.class);
        IUrlResolver secondResolver = mock(IUrlResolver.class);

        when(firstResolver.canResolve(resource)).thenReturn(true);
        when(firstResolver.resolve(resource)).thenReturn(new ByteArrayInputStream(new byte[0]));

        when(secondResolver.canResolve(resource)).thenReturn(true);
        when(secondResolver.resolve(resource)).thenReturn(new ByteArrayInputStream(expectedContent));

        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class)) {
            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class)))
                    .thenReturn(new byte[0])
                    .thenReturn(expectedContent);

            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(List.of(firstResolver, secondResolver));

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(expectedContent, result);
        }
    }

    @ParameterizedTest
    @MethodSource("nullMimeTypeScenarios")
    void isUnexpectedlyResolvedAsHtmlWithNullMimeTypes(String detectedMimeType, String expectedMimeType) {
        String resource = "file.txt";
        byte[] content = "content".getBytes();

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn(detectedMimeType);
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn(expectedMimeType);

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertFalse(result);
        }
    }

    private static Stream<Arguments> nullMimeTypeScenarios() {
        return Stream.of(
                Arguments.of(null, "text/plain"),           // detected null
                Arguments.of("text/plain", null),           // expected null
                Arguments.of(null, null)                    // both null
        );
    }

    @Test
    void isUnexpectedlyResolvedAsHtmlWithXhtmlMatchingTypes() {
        String resource = "file.xhtml";
        byte[] content = "<html xmlns=\"http://www.w3.org/1999/xhtml\"></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn(MediaType.APPLICATION_XHTML_XML);
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn(MediaType.APPLICATION_XHTML_XML);

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void isUnexpectedlyResolvedAsHtmlWithXhtmlMismatchingTypes() {
        String resource = "file.html";
        byte[] content = "<html xmlns=\"http://www.w3.org/1999/xhtml\"></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn(MediaType.APPLICATION_XHTML_XML);
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("text/html");

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertTrue(result);
        }
    }

    @Test
    void isUnexpectedlyResolvedAsHtmlWithNonXhtmlTypes() {
        String resource = "image.png";
        byte[] content = new byte[]{0x00, 0x01, 0x02};

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("image/png");
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("text/plain");

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void isUnexpectedlyResolvedAsHtmlWithMatchingNonXhtmlTypes() {
        String resource = "image.png";
        byte[] content = new byte[]{0x00, 0x01, 0x02};

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("image/png");
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("image/png");

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void isUnexpectedlyResolvedAsHtmlWithEmptyContent() {
        String resource = "file.txt";
        byte[] content = new byte[0];

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("application/octet-stream");
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("text/plain");

            boolean result = resourceProvider.isUnexpectedlyResolvedAsHtml(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void getWorkItemIdFromAttachmentUrlValidUrl() {
        String url = "http://localhost/polarion/wi-attachment/elibrary/EL-14852/attachment.png";
        String result = resourceProvider.getWorkItemIdsWithUnavailableAttachments(url);
        assertEquals("EL-14852", result);
    }

    @Test
    void getWorkItemIdFromAttachmentUrlInvalidUrl() {
        String url = "/http://example.com/invalid/url";
        String result = resourceProvider.getWorkItemIdsWithUnavailableAttachments(url);
        assertNull(result);
    }

    @Test
    @SneakyThrows
    void getDefaultContentEmptyForNonImage() {
        String resource = "attachment.txt";
        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getImageFormat(resource))
                    .thenReturn("");

            byte[] content = resourceProvider.getDefaultContent(resource);
            assertArrayEquals(new byte[0], content);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPolarionUrlResolverWithoutGenericUrlChildResolver() throws Exception {
        ParentUrlResolver parentUrlResolverMock = mock(ParentUrlResolver.class);
        Field childResolversField = ParentUrlResolver.class.getDeclaredField("childResolvers");
        childResolversField.setAccessible(true);

        IUrlResolver mockResolver1 = mock(IUrlResolver.class);
        IUrlResolver mockResolver2 = mock(GenericUrlResolver.class);
        IUrlResolver mockResolver3 = mock(IUrlResolver.class);

        List<IUrlResolver> childResolvers = List.of(mockResolver1, mockResolver2, mockResolver3);
        childResolversField.set(parentUrlResolverMock, childResolvers);

        try (MockedStatic<PolarionUrlResolver> polarionUrlResolverMock = mockStatic(PolarionUrlResolver.class)) {
            polarionUrlResolverMock.when(PolarionUrlResolver::getInstance).thenReturn(parentUrlResolverMock);

            IAttachmentUrlResolver result = resourceProvider.getPolarionUrlResolverWithoutGenericUrlChildResolver();

            assertTrue(result instanceof ParentUrlResolver);
            ParentUrlResolver resultParent = (ParentUrlResolver) result;

            List<IUrlResolver> filteredResolvers =
                    (List<IUrlResolver>) childResolversField.get(resultParent);

            assertEquals(2, filteredResolvers.size());
            assertTrue(filteredResolvers.contains(mockResolver1));
            assertTrue(filteredResolvers.contains(mockResolver3));
            assertFalse(filteredResolvers.contains(mockResolver2));
        }
    }
}
