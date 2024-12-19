package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.tracker.internal.url.GenericUrlResolver;
import com.polarion.alm.tracker.internal.url.IAttachmentUrlResolver;
import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.alm.tracker.internal.url.ParentUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({
        MockitoExtension.class,
        TransactionalExecutorExtension.class,
        PlatformContextMockExtension.class
})
class PdfExporterFileResourceProviderTest {
    @Mock
    PdfExporterFileResourceProvider resourceProviderMock;

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
    void processPossibleSvgImageTest() {
        byte[] basicString = "basic".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(basicString, resourceProvider.processPossibleSvgImage(basicString));
    }

    @Test
    @SneakyThrows
    void replaceImagesAsBase64EncodedTest() {
        byte[] imgBytes;
        try (InputStream is = this.getClass().getResourceAsStream("/test_img.png")) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }

        when(resourceProviderMock.getResourceAsBytes("http://localhost/some-path/img.png", null)).thenReturn(imgBytes);
        when(resourceProviderMock.getResourceAsBase64String(any(), eq(null))).thenCallRealMethod();
        String result = resourceProviderMock.getResourceAsBase64String("http://localhost/some-path/img.png", null);
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes), result);
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesSuccess() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(true);
        when(resolverMock.resolve(TEST_RESOURCE)).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        List<String> unavailableAttachments = new ArrayList<>();
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE, unavailableAttachments);

        assertArrayEquals(TEST_CONTENT, result);
        assertTrue(unavailableAttachments.isEmpty());
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesNoResolverFound() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(false);

        List<String> unavailableAttachments = new ArrayList<>();
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE, unavailableAttachments);

        assertArrayEquals(new byte[0], result);
        assertTrue(unavailableAttachments.isEmpty());
    }

    @Test
    @SneakyThrows
    void getResourceAsBytesResolverReturnsNull() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenReturn(true);
        when(resolverMock.resolve(TEST_RESOURCE)).thenReturn(null);

        List<String> unavailableAttachments = new ArrayList<>();
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE, unavailableAttachments);

        assertArrayEquals(new byte[0], result);
        assertTrue(unavailableAttachments.isEmpty());
    }

    @Test
    void getResourceAsBytesExceptionHandling() {
        when(resolverMock.canResolve(TEST_RESOURCE)).thenThrow(new RuntimeException("Test Exception"));

        List<String> unavailableAttachments = new ArrayList<>();
        byte[] result = resourceProvider.getResourceAsBytes(TEST_RESOURCE, unavailableAttachments);

        assertArrayEquals(new byte[0], result);
        assertTrue(unavailableAttachments.isEmpty());
    }

    @Test
    void getWorkItemIdFromAttachmentUrlValidUrl() {
        String url = "http://localhost/polarion/wi-attachment/elibrary/EL-14852/attachment.png";
        String result = resourceProvider.getWorkItemIdFromAttachmentUrl(url);
        assertEquals("EL-14852", result);
    }

    @Test
    void getWorkItemIdFromAttachmentUrl_InvalidUrl() {
        String url = "http://example.com/invalid/url";
        String result = resourceProvider.getWorkItemIdFromAttachmentUrl(url);
        assertNull(result);
    }

    @Test
    void isMediaTypeMismatchMatchingMimeTypes() {
        String resource = "image.png";
        byte[] content = new byte[0];

        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByContent(resource, content))
                    .thenReturn("image/png");
            mockedMediaUtils.when(() -> MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null))
                    .thenReturn("image/png");

            boolean result = resourceProviderMock.isMediaTypeMismatch(resource, content);
            assertFalse(result);
        }
    }

    @Test
    @SneakyThrows
    void getDefaultContentEmptyForNonImage() {
        String resource = "attachment.txt";
        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            mockedMediaUtils.when(() -> MediaUtils.getImageFormat(resource))
                    .thenReturn("");

            byte[] content = resourceProviderMock.getDefaultContent(resource);
            assertNull(content);
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
