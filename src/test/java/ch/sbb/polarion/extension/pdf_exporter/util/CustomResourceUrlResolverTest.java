package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.util.ResourceUrlPolicy.Mode;
import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, BundleJarsPrioritizingRunnableMockExtension.class})
class CustomResourceUrlResolverTest {

    // the content has to be what its sender calls it, so the bytes of the tests are the bytes of a png
    private static final byte[] PNG_CONTENT = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

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
    @SneakyThrows
    void skipsContentWhichIsNoResourceWhenNothingWasSaidAboutIt() {
        // an internal service answering a forged request tends to say nothing, or to say octet-stream,
        // and to hand over a text: neither an image, nor a font, nor a stylesheet
        respond("/secrets", null, "the answer of an internal service, plain text\n".getBytes(StandardCharsets.UTF_8), false);
        respond("/blob", "application/octet-stream", "id,name\n1,admin\n".getBytes(StandardCharsets.UTF_8), false);

        assertNull(resolver(16).resolveImpl(toUrl(url("/secrets"))));
        assertNull(resolver(16).resolveImpl(toUrl(url("/blob"))));
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
        when(configuration.getExternalResourcesPolicy()).thenReturn("ALLOW_ALL");

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
    @SneakyThrows
    void keepsTheAnswerOfTheFirstSchemeOfANetworkPathReference() {
        // the host answered, and 404 is an answer: asking it again over the other scheme adds nothing
        status("/missing.png", 404);
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), "http://localhost", 16);
        CustomResourceUrlResolver resolver = spy(new CustomResourceUrlResolver(policy));

        assertNull(resolver.resolve("//127.0.0.1:" + server.getAddress().getPort() + "/missing.png"));

        verify(resolver, times(1)).resolveImpl(any());
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
    void triesTheOtherSchemeWhereTheHostDoesNotSpeakTlsAtAll() {
        CustomResourceUrlResolver resolver = resolver(16);

        // a peer which is not speaking tls answered nothing, and the other scheme is the next question
        assertFalse(resolver.isCertificateFailure(new SSLException("Unrecognized SSL message, plaintext connection?")));
        // a refused certificate is an answer, and never one to repeat in the clear
        assertTrue(resolver.isCertificateFailure(new SSLHandshakeException("failed") {
            @Override
            public synchronized Throwable getCause() {
                return new CertificateException("no trusted certificate found");
            }
        }));
        assertTrue(resolver.isCertificateFailure(new SSLPeerUnverifiedException("hostname mismatch")));
    }

    @Test
    @SneakyThrows
    void fetchesATrustedHostThroughAProxyWhichResolvesItsName() {
        // the egress of such a server is the proxy, and the names it fetches are the proxy's to resolve:
        // a host the configuration trusts must not need an address of its own here
        try (ServerSocket proxy = new ServerSocket(0)) {
            AtomicBoolean reached = new AtomicBoolean();
            Thread listener = new Thread(() -> {
                try (Socket accepted = proxy.accept()) {
                    reached.set(accepted.getInputStream().read() >= 0);
                } catch (IOException e) {
                    // the test asks whether the request arrived at the proxy, and nothing else
                }
            });
            listener.setDaemon(true);
            listener.start();
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", String.valueOf(proxy.getLocalPort()));
            try {
                ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOWLIST_ONLY, List.of("cdn.invalid"), null, 16);

                assertNull(new CustomResourceUrlResolver(policy).resolve("http://cdn.invalid/img.png"));

                listener.join(5000);
                assertTrue(reached.get());
            } finally {
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
            }
        }
    }

    @Test
    @SneakyThrows
    void keepsFetchingWhenOnlyASocksProxyIsConfigured() {
        // the route planner of the client ignores a socks proxy, so the request stays direct and pinned
        respond("/img.png", "image/png", PNG_CONTENT, false);
        System.setProperty("socksProxyHost", "socks.invalid");
        System.setProperty("socksProxyPort", "1080");
        try {
            ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), null, 16);

            try (InputStream stream = new CustomResourceUrlResolver(policy).resolveImpl(toUrl(url("/img.png")))) {
                assertNotNull(stream);
                assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
            }
            // and the classification itself, asked about an address no default non-proxy list covers
            assertFalse(new CustomResourceUrlResolver(policy).decideProxy(ProxySelector.getDefault(), toUrl("http://8.8.8.8/img.png")).proxied());
        } finally {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
    }

    @Test
    @SneakyThrows
    void sendsARequestThroughASocksProxyAtTheSocket() {
        // the client plans no route to a socks proxy, the jvm applies it under the connection, so a
        // socks setup keeps working: the request reaches the proxy rather than the host directly
        try (ServerSocket socksProxy = new ServerSocket(0)) {
            SocksRequest seen = new SocksRequest();
            Thread listener = readSocksRequest(socksProxy, seen);
            System.setProperty("socksProxyHost", "127.0.0.1");
            System.setProperty("socksProxyPort", String.valueOf(socksProxy.getLocalPort()));
            try {
                ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), null, 16);

                assertNull(new CustomResourceUrlResolver(policy).resolve("http://8.8.8.8/img.png"));

                listener.join(5000);
                assertTrue(seen.reached.get());
                // and it names an address, never a host name the proxy would resolve on its own
                assertEquals(SOCKS_ADDRESS_TYPE_IPV4, seen.addressType.get());
                assertEquals("8.8.8.8", seen.address.get());
            } finally {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            }
        }
    }

    @Test
    @SneakyThrows
    void namesTheVettedAddressToASocksProxyRatherThanTheHost() {
        // a proxy which resolves the host name itself would decide the address, and that is what this
        // asks: the request names an address, so the name never reaches the proxy
        try (ServerSocket socksProxy = new ServerSocket(0)) {
            SocksRequest seen = new SocksRequest();
            Thread listener = readSocksRequest(socksProxy, seen);
            System.setProperty("socksProxyHost", "127.0.0.1");
            System.setProperty("socksProxyPort", String.valueOf(socksProxy.getLocalPort()));
            // the jvm keeps loopback out of every proxy by default, and here the proxy is the point
            System.setProperty("socksNonProxyHosts", "");
            try {
                ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), null, 16);

                assertNull(new CustomResourceUrlResolver(policy).resolve("http://localhost:" + server.getAddress().getPort() + "/img.png"));

                listener.join(5000);
                assertTrue(seen.reached.get());
                assertEquals(SOCKS_ADDRESS_TYPE_IPV4, seen.addressType.get());
                assertEquals("127.0.0.1", seen.address.get());
            } finally {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                System.clearProperty("socksNonProxyHosts");
            }
        }
    }

    /**
     * Answers the greeting of a socks 5 client and keeps what its request names, then closes.
     */
    private Thread readSocksRequest(ServerSocket socksProxy, SocksRequest seen) {
        Thread listener = new Thread(() -> {
            try (Socket accepted = socksProxy.accept()) {
                InputStream in = accepted.getInputStream();
                in.read();
                int methods = in.read();
                in.readNBytes(Math.max(methods, 0));
                accepted.getOutputStream().write(new byte[]{5, 0});
                accepted.getOutputStream().flush();
                byte[] head = in.readNBytes(4);
                seen.reached.set(head.length == 4);
                seen.addressType.set(head.length == 4 ? head[3] : -1);
                if (head.length == 4 && head[3] == SOCKS_ADDRESS_TYPE_IPV4) {
                    seen.address.set(InetAddress.getByAddress(in.readNBytes(4)).getHostAddress());
                }
            } catch (IOException e) {
                // the test asks what the request named, and a closed connection names nothing
            }
        });
        listener.setDaemon(true);
        listener.start();
        return listener;
    }

    private static final int SOCKS_ADDRESS_TYPE_IPV4 = 1;

    private static final class SocksRequest {
        private final AtomicBoolean reached = new AtomicBoolean();
        private final AtomicInteger addressType = new AtomicInteger(-1);
        private final AtomicReference<String> address = new AtomicReference<>();
    }

    @Test
    @SneakyThrows
    void treatsASelectorWhichCannotAnswerAsAProxy() {
        // a check which cannot be made is not a check that passed, so such a url is skipped as proxied
        ProxySelector previous = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                throw new IllegalStateException("no answer");
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, java.io.IOException failure) {
                // nothing to report, this selector never answers in the first place
            }
        });
        try {
            ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of(), null, 16);
            CustomResourceUrlResolver resolver = new CustomResourceUrlResolver(policy);

            assertTrue(resolver.decideProxy(ProxySelector.getDefault(), toUrl("http://8.8.8.8/img.png")).proxied());
            assertNull(resolver.resolveImpl(toUrl("http://8.8.8.8/img.png")));

            // and a trusted host is no reason to send the request past the route the configuration names
            ResourceUrlPolicy trusting = new ResourceUrlPolicy(Mode.ALLOWLIST_ONLY, List.of("8.8.8.8"), null, 16);
            assertNull(new CustomResourceUrlResolver(trusting).resolveImpl(toUrl("http://8.8.8.8/img.png")));
        } finally {
            ProxySelector.setDefault(previous);
        }
    }

    @Test
    @SneakyThrows
    void routesByTheSelectorAndNotByTheProperty() {
        // the check reads the selector, so the routes have to come from there too: a selector answering
        // no proxy wins over http.proxyHost, and a request under it stays direct as the check said
        respond("/img.png", "image/png", PNG_CONTENT, false);
        System.setProperty("http.proxyHost", "proxy.invalid");
        System.setProperty("http.proxyPort", "3128");
        ProxySelector previous = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(Proxy.NO_PROXY);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, java.io.IOException failure) {
                // nothing to report, the test asks for a route and never for a failure
            }
        });
        try {
            ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOW_ALL, List.of(), null, 16);

            try (InputStream stream = new CustomResourceUrlResolver(policy).resolveImpl(toUrl(url("/img.png")))) {
                assertNotNull(stream);
                assertArrayEquals(PNG_CONTENT, stream.readAllBytes());
            }
        } finally {
            ProxySelector.setDefault(previous);
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    @Test
    void readsTheProxyListAsTheJvmReadsIt() {
        // a pac file writing 'DIRECT; PROXY host:port' means direct, and the entry after it is a fallback
        CustomResourceUrlResolver resolver = resolver(16);
        HttpHost proxyHost = new HttpHost("proxy.invalid", 3128);

        assertFalse(resolver.decideProxy(selectorOf(Proxy.NO_PROXY, httpProxy()), toUrl("http://8.8.8.8/i.png")).proxied());
        assertTrue(resolver.decideProxy(selectorOf(httpProxy(), Proxy.NO_PROXY), toUrl("http://8.8.8.8/i.png")).proxied());
        // a socks entry is no route the client plans, so it decides nothing either way
        assertEquals(proxyHost.toHostString(),
                resolver.decideProxy(selectorOf(socksProxy(), httpProxy()), toUrl("http://8.8.8.8/i.png")).host().toHostString());
        assertFalse(resolver.decideProxy(selectorOf(socksProxy()), toUrl("http://8.8.8.8/i.png")).proxied());
    }

    private Proxy httpProxy() {
        return new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("proxy.invalid", 3128));
    }

    private Proxy socksProxy() {
        return new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("socks.invalid", 1080));
    }

    private ProxySelector selectorOf(Proxy... proxies) {
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(proxies);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException failure) {
                // the test asks what the list says, and never reports a failure to it
            }
        };
    }

    @Test
    void classifiesAnHttpProxyAsARoute() {
        System.setProperty("http.proxyHost", "proxy.invalid");
        System.setProperty("http.proxyPort", "3128");
        try {
            CustomResourceUrlResolver resolver = resolver(16);

            assertTrue(resolver.decideProxy(ProxySelector.getDefault(), toUrl("http://8.8.8.8/img.png")).proxied());
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
