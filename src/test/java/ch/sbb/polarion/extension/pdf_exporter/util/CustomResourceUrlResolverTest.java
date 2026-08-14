package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.util.ResourceUrlPolicy.Mode;
import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, BundleJarsPrioritizingRunnableMockExtension.class})
class CustomResourceUrlResolverTest {

    private static final byte[] PNG_CONTENT = "not really a png".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    @SneakyThrows
    void startServer() {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @SneakyThrows
    private URL toUrl(String value) {
        return URI.create(value).toURL();
    }

    private void respond(String path, String contentType, byte[] body, boolean chunked) {
        server.createContext(path, exchange -> {
            requestCount.incrementAndGet();
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(200, chunked ? 0 : body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    private void redirect(String path, String location) {
        server.createContext(path, exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
    }

    private void status(String path, int code) {
        server.createContext(path, (HttpExchange exchange) -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        });
    }

    private CustomResourceUrlResolver resolver(int maxSizeMB) {
        return new CustomResourceUrlResolver(new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), null, maxSizeMB));
    }

    @Test
    @SneakyThrows
    void replaceImagesUrlUnderscoreAndSpaceReplacementTest() {
        CustomResourceUrlResolver resolver = mock(CustomResourceUrlResolver.class);
        InputStream is = mock(InputStream.class);
        when(resolver.resolve(any())).thenCallRealMethod();
        when(resolver.resolveImpl(any())).thenReturn(is);
        try (InputStream is1 = resolver.resolve("http://localhost/some path/img%5Fname.png")) {
            try (InputStream is2 = verify(resolver, times(1)).resolveImpl(URI.create("http://localhost/some%20path/img_name.png").toURL())) {
                assertEquals(is, is1);
                assertNull(is2);
            }
        }
    }

    @Test
    @SneakyThrows
    void readsAllowedResource() {
        respond("/img.png", "image/png", PNG_CONTENT, false);
        try (InputStream stream = resolver(16).resolveImpl(toUrl(url("/img.png")))) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
        }
    }

    @Test
    void skipsResourceRejectedByPolicy() {
        respond("/img.png", "image/png", PNG_CONTENT, false);
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of(), null, 16);
        assertNull(new CustomResourceUrlResolver(policy).resolveImpl(toUrl(url("/img.png"))));
        assertEquals(0, requestCount.get());
    }

    @Test
    void skipsForeignContentType() {
        respond("/secret", "application/json", "{\"internal\":\"api response\"}".getBytes(StandardCharsets.UTF_8), false);
        assertNull(resolver(16).resolveImpl(toUrl(url("/secret"))));
    }

    @Test
    void skipsDocumentDisguisedAsOctetStream() {
        respond("/secret", "application/octet-stream", "<html><body>internal page</body></html>".getBytes(StandardCharsets.UTF_8), false);
        assertNull(resolver(16).resolveImpl(toUrl(url("/secret"))));
    }

    @Test
    @SneakyThrows
    void readsImageWithoutContentType() {
        respond("/img.png", null, PNG_CONTENT, false);
        try (InputStream stream = resolver(16).resolveImpl(toUrl(url("/img.png")))) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
        }
    }

    @Test
    void skipsResourceExceedingDeclaredSize() {
        respond("/img.png", "image/png", PNG_CONTENT, false);
        assertNull(resolver(0).resolveImpl(toUrl(url("/img.png"))));
    }

    @Test
    void skipsResourceExceedingStreamedSize() {
        respond("/img.png", "image/png", PNG_CONTENT, true);
        assertNull(resolver(0).resolveImpl(toUrl(url("/img.png"))));
    }

    @Test
    @SneakyThrows
    void followsRedirect() {
        redirect("/old.png", "/img.png");
        respond("/img.png", "image/png", PNG_CONTENT, false);
        try (InputStream stream = resolver(16).resolveImpl(toUrl(url("/old.png")))) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
            assertEquals(2, requestCount.get());
        }
    }

    @Test
    void stopsOnRedirectLoop() {
        redirect("/loop.png", url("/loop.png"));
        assertNull(resolver(16).resolveImpl(toUrl(url("/loop.png"))));
        assertEquals(6, requestCount.get());
    }

    @Test
    void skipsRedirectWithoutLocation() {
        server.createContext("/no-location.png", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        assertNull(resolver(16).resolveImpl(toUrl(url("/no-location.png"))));
    }

    @Test
    @SneakyThrows
    void followsPermanentRedirect() {
        server.createContext("/old.png", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Location", "/img.png");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        respond("/img.png", "image/png", PNG_CONTENT, false);
        try (InputStream stream = resolver(16).resolveImpl(toUrl(url("/old.png")))) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
        }
    }

    @Test
    void skipsRedirectWithBlankLocation() {
        redirect("/blank.png", "  ");
        assertNull(resolver(16).resolveImpl(toUrl(url("/blank.png"))));
    }

    @Test
    void takesItsPolicyFromTheConfiguration() {
        PdfExporterExtensionConfiguration configuration = mock(PdfExporterExtensionConfiguration.class);
        when(configuration.getExternalResourcesPolicy()).thenReturn("allowAll");

        try (MockedStatic<PdfExporterExtensionConfiguration> mocked = mockStatic(PdfExporterExtensionConfiguration.class)) {
            mocked.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);
            respond("/img.png", "image/png", PNG_CONTENT, false);

            assertNotNull(new CustomResourceUrlResolver().resolveImpl(toUrl(url("/img.png"))));
        }
    }

    @Test
    void skipsRedirectToBlockedTarget() {
        redirect("/old.png", "http://169.254.169.254/latest/meta-data/");
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of("127.0.0.1:" + server.getAddress().getPort()), null, 16);
        assertNull(new CustomResourceUrlResolver(policy).resolveImpl(toUrl(url("/old.png"))));
        assertEquals(1, requestCount.get());
    }

    @Test
    @SneakyThrows
    void resolvesANetworkPathReferenceAgainstItsOwnHost() {
        // '//127.0.0.1:port/img.png' names its own host, only the scheme comes from the base url
        respond("/img.png", "image/png", PNG_CONTENT, false);
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), "http://localhost", 16);
        CustomResourceUrlResolver resolver = new CustomResourceUrlResolver(policy);

        try (InputStream stream = resolver.resolve("//127.0.0.1:" + server.getAddress().getPort() + "/img.png")) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
        }
    }

    @Test
    @SneakyThrows
    void fallsBackToTheOtherSchemeOfANetworkPathReference() {
        // the base url says https, the server speaks http: the second attempt has to find it
        respond("/img.png", "image/png", PNG_CONTENT, false);
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), "https://localhost", 16);
        CustomResourceUrlResolver resolver = new CustomResourceUrlResolver(policy);

        try (InputStream stream = resolver.resolve("//127.0.0.1:" + server.getAddress().getPort() + "/img.png")) {
            assertNotNull(stream);
            assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
        }
    }

    @Test
    void skipsAProxiedResourceWhichIsNotExplicitlyTrusted() {
        // a proxy resolves the host name itself, so the vetted addresses would decide nothing
        System.setProperty("http.proxyHost", "proxy.invalid");
        System.setProperty("http.proxyPort", "3128");
        try {
            ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of(), null, 16);

            assertNull(new CustomResourceUrlResolver(policy).resolveImpl(toUrl("http://8.8.8.8/img.png")));
        } finally {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    @Test
    @SneakyThrows
    void keepsFetchingAHostTheProxyIsNotUsedFor() {
        respond("/img.png", "image/png", PNG_CONTENT, false);
        System.setProperty("http.proxyHost", "proxy.invalid");
        System.setProperty("http.proxyPort", "3128");
        try {
            ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL,
                    List.of("127.0.0.1:" + server.getAddress().getPort()), null, 16);

            try (InputStream stream = new CustomResourceUrlResolver(policy).resolveImpl(toUrl(url("/img.png")))) {
                assertNotNull(stream);
                assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
            }
        } finally {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    @Test
    void skipsErrorResponse() {
        status("/missing.png", 404);
        assertNull(resolver(16).resolveImpl(toUrl(url("/missing.png"))));
    }

    @Test
    void resolveReturnsNullOnFailure() {
        assertNull(resolver(16).resolve("http://"));
    }

    @Test
    void canResolveOnlyHttpAndAbsolutePaths() {
        CustomResourceUrlResolver resolver = resolver(16);
        assertTrue(resolver.canResolve("/polarion/img.png"));
        assertTrue(resolver.canResolve("http://example.com/img.png"));
        assertTrue(resolver.canResolve("https://example.com/img.png"));
        assertFalse(resolver.canResolve("data:image/png;base64,AAAA"));
    }
}
