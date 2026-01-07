package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomResourceUrlResolverTest {

    private static final String BASE_URL = "http://localhost:8080/polarion";

    private CustomResourceUrlResolver resolver;
    private String originalBaseUrl;

    @BeforeEach
    void setUp() {
        resolver = new CustomResourceUrlResolver();
        originalBaseUrl = System.getProperty("base.url");
        System.setProperty("base.url", BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (originalBaseUrl != null) {
            System.setProperty("base.url", originalBaseUrl);
        } else {
            System.clearProperty("base.url");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/some/path", "http://example.com", "https://example.com"})
    void canResolveShouldReturnTrueForValidUrls(String url) {
        assertTrue(resolver.canResolve(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "file:///path", "mailto:test@example.com", "relative/path"})
    void canResolveShouldReturnFalseForInvalidUrls(String url) {
        assertFalse(resolver.canResolve(url));
    }

    @Test
    @SneakyThrows
    void resolveShouldNormalizeUrlAndCallResolveImpl() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        InputStream mockStream = mock(InputStream.class);
        doReturn(mockStream).when(spyResolver).resolveImpl(any());

        InputStream result = spyResolver.resolve("http://localhost/some path/img%5Fname.png");

        assertEquals(mockStream, result);
        verify(spyResolver).resolveImpl(URI.create("http://localhost/some%20path/img_name.png"));
    }

    @Test
    @SneakyThrows
    void resolveShouldPrependBaseUrlForRelativePaths() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        InputStream mockStream = mock(InputStream.class);
        doReturn(mockStream).when(spyResolver).resolveImpl(any());

        spyResolver.resolve("/relative/path/image.png");

        verify(spyResolver).resolveImpl(URI.create(BASE_URL + "/relative/path/image.png"));
    }

    @Test
    @SneakyThrows
    void resolveShouldHandleBaseUrlWithTrailingSlash() {
        System.setProperty("base.url", BASE_URL + "/");
        CustomResourceUrlResolver spyResolver = spy(new CustomResourceUrlResolver());
        InputStream mockStream = mock(InputStream.class);
        doReturn(mockStream).when(spyResolver).resolveImpl(any());

        spyResolver.resolve("/relative/path");

        verify(spyResolver).resolveImpl(URI.create(BASE_URL + "/relative/path"));
    }

    @Test
    void resolveShouldReturnNullOnException() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        doThrow(new RuntimeException("Connection failed")).when(spyResolver).resolveImpl(any());

        InputStream result = spyResolver.resolve("http://localhost/test.png");

        assertNull(result);
    }

    @Test
    void canResolveShouldReturnTrueForHttpRedirectLocations() {
        // Test that redirect locations can be resolved
        assertTrue(resolver.canResolve("http://redirected.com/new-path"));
        assertTrue(resolver.canResolve("https://redirected.com/new-path"));
        assertTrue(resolver.canResolve("/relative/redirect"));
    }

    @Test
    void resolveShouldHandleSpacesInUrl() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        doReturn(mock(InputStream.class)).when(spyResolver).resolveImpl(any());

        spyResolver.resolve("http://localhost/path with spaces/file.png");

        verify(spyResolver).resolveImpl(URI.create("http://localhost/path%20with%20spaces/file.png"));
    }

    @Test
    void resolveShouldReplaceEncodedUnderscores() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        doReturn(mock(InputStream.class)).when(spyResolver).resolveImpl(any());

        spyResolver.resolve("http://localhost/img%5Fname.png");

        verify(spyResolver).resolveImpl(URI.create("http://localhost/img_name.png"));
    }

    @Test
    @SneakyThrows
    void resolveShouldHandleCombinedNormalization() {
        CustomResourceUrlResolver spyResolver = spy(resolver);
        InputStream mockStream = mock(InputStream.class);
        doReturn(mockStream).when(spyResolver).resolveImpl(any());

        InputStream result = spyResolver.resolve("http://localhost/some path/img%5Fname.png");

        assertEquals(mockStream, result);
        verify(spyResolver).resolveImpl(URI.create("http://localhost/some%20path/img_name.png"));
    }

    @Test
    void canResolveShouldHandleEmptyString() {
        assertFalse(resolver.canResolve(""));
    }

    @Test
    void resolveShouldHandleInvalidUri() {
        // Invalid URI should be caught and return null
        InputStream result = resolver.resolve("http://[invalid");
        assertNull(result);
    }
}
