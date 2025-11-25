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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

            List<IUrlResolver> resolvers = List.of(resolver);
            List<String> unavailableWorkItemAttachments = new ArrayList<>();

            PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider(resolvers);

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(expectedBytes, result);
            assertTrue(unavailableWorkItemAttachments.isEmpty());
        }
    }

    @Test
    void isRedirectToLoginPageNonHtmlContent() {
        String resource = "image.png";
        byte[] content = new byte[0];

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("image/png");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void isRedirectToLoginPageWithXhtmlLoginPage() {
        String resource = "attachment.html";
        byte[] content = "<html><head><title>Login</title></head></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("application/xhtml+xml");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertTrue(result);
        }
    }

    @Test
    void isRedirectToLoginPageWithHtmlLoginPage() {
        String resource = "page.html";
        byte[] content = "<html><head><title>  Login  </title></head></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("text/html");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertTrue(result);
        }
    }

    @Test
    void isRedirectToLoginPageWithXhtmlButNotLoginPage() {
        String resource = "attachment.html";
        byte[] content = "<html><head><title>Regular Page</title></head></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("application/xhtml+xml");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertFalse(result);
        }
    }

    @Test
    void isRedirectToLoginPageCaseInsensitive() {
        String resource = "page.html";
        byte[] content = "<html><head><title>LoGiN</title></head></html>".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("text/html");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertTrue(result);
        }
    }

    @Test
    void isRedirectToLoginPageEmptyContent() {
        String resource = "page.html";
        byte[] content = new byte[0];

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("text/html");

            boolean result = resourceProvider.isRedirectToLoginPage(resource, content);
            assertFalse(result);
        }
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesImplWithLoginPageRedirect() {
        try (MockedStatic<StreamUtils> streamUtilsMockedStatic = mockStatic(StreamUtils.class);
             MockedStatic<WorkItemAttachmentUrlResolver> workItemAttachmentUrlResolverMockedStatic = mockStatic(WorkItemAttachmentUrlResolver.class);
             MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class);
             MockedStatic<ExportContext> exportContextMockedStatic = mockStatic(ExportContext.class)) {

            String resource = "/polarion/wi-attachment/project/WI-123/attachment.png";
            byte[] resolvedBytes = "<html><head><title>Login</title></head></html>".getBytes(StandardCharsets.UTF_8);
            byte[] defaultBytes = "default".getBytes();

            streamUtilsMockedStatic.when(() -> StreamUtils.suckStreamThenClose(any(InputStream.class)))
                    .thenReturn(resolvedBytes);
            workItemAttachmentUrlResolverMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource))
                    .thenReturn(true);
            workItemAttachmentUrlResolverMockedStatic.when(() -> WorkItemAttachmentUrlResolver.isSvg(resource))
                    .thenReturn(false);

            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, resolvedBytes))
                    .thenReturn("text/html");

            IUrlResolver resolver = mock(IUrlResolver.class);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(new ByteArrayInputStream(resolvedBytes));

            PdfExporterFileResourceProvider fileResourceProvider = spy(new PdfExporterFileResourceProvider(List.of(resolver)));
            doReturn(defaultBytes).when(fileResourceProvider).getDefaultContent(resource);
            doReturn("WI-123").when(fileResourceProvider).getWorkItemIdsWithUnavailableAttachments(resource);

            byte[] result = fileResourceProvider.getResourceAsBytesImpl(resource);

            assertArrayEquals(defaultBytes, result);
            exportContextMockedStatic.verify(() -> ExportContext.addWorkItemIDsWithMissingAttachment("WI-123"));
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
