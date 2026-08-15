package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.net.ssl.SSLPeerUnverifiedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

/**
 * Custom version of {@link com.polarion.alm.tracker.internal.url.GenericUrlResolver} with the redirect support.
 * <p>
 * Every request passes {@link ResourceUrlPolicy}, which keeps a document editor from making the server
 * fetch an arbitrary address and from getting the response body back in the exported document. The
 * connection is pinned to the addresses the policy vetted, so a second name resolution cannot reach an
 * address the policy never saw.
 * </p>
 */
public class CustomResourceUrlResolver implements IUrlResolver {

    private static final Logger logger = Logger.getLogger(CustomResourceUrlResolver.class);

    private static final int CONNECTION_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;
    private static final int MAX_REDIRECTS = 5;
    private static final int BUFFER_SIZE = 8192;
    private static final String SKIPPED_RESOURCE = "Skipped resource ";
    // every redirect a client follows, the permanent and the temporary ones of both generations
    private static final Set<Integer> REDIRECT_STATUS_CODES = Set.of(HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER, 307, 308);
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    private final ResourceUrlPolicy policy;

    public CustomResourceUrlResolver() {
        this(ResourceUrlPolicy.getInstance());
    }

    public CustomResourceUrlResolver(@NotNull ResourceUrlPolicy policy) {
        this.policy = policy;
    }

    public boolean canResolve(@NotNull String url) {
        return Stream.of("/", "http:", "https:").anyMatch(url::startsWith);
    }

    public InputStream resolve(@NotNull String urlStr) {
        try {
            if (MediaUtils.isNetworkPathReference(urlStr)) {
                return resolveNetworkPathReference(urlStr);
            }
            return resolveImpl(URI.create(normalizeUrl(ensureAbsoluteUrl(urlStr))).toURL());
        } catch (Exception e) {
            logger.warn("Failed to load resource: " + urlStr, e);
        }
        return null;
    }

    /**
     * A reference like {@code //host/path} takes the scheme of the base url the document carries, and
     * that base is built elsewhere, from the cluster host among other things. Rather than guessing which
     * one it ends up with, both are tried, each one checked by the policy on its own.
     */
    private InputStream resolveNetworkPathReference(@NotNull String urlStr) {
        String preferredScheme = policy.getBaseUrlScheme();
        String otherScheme = HTTPS_SCHEME.equals(preferredScheme) ? HTTP_SCHEME : HTTPS_SCHEME;
        SchemeAttempt preferred = resolveWithScheme(preferredScheme, urlStr);
        if (preferred.conclusive()) {
            // the host answered, and what it answered was read or refused: the other scheme adds nothing,
            // and a host which speaks tls badly is not asked for the same resource in the clear either
            return preferred.stream();
        }
        SchemeAttempt other = resolveWithScheme(otherScheme, urlStr);
        if (other.stream() == null) {
            logger.warn(SKIPPED_RESOURCE + urlStr + ": neither " + preferredScheme + " nor " + otherScheme + " could read it");
        }
        return other.stream();
    }

    private SchemeAttempt resolveWithScheme(@NotNull String scheme, @NotNull String urlStr) {
        try {
            URL url = URI.create(normalizeUrl(scheme + ":" + urlStr)).toURL();
            InputStream stream = resolveImpl(url);
            // a decision was taken, whether it produced a resource or refused one, unless the refusal
            // itself turned on the scheme: an allowed host carrying a port is allowed under that port only
            return new SchemeAttempt(stream, stream != null || !policy.isRefusalPortSpecific(url));
        } catch (Exception e) {
            logger.debug("Failed to load resource " + scheme + ":" + urlStr + ": " + e.getMessage());
            // nothing was decided unless the peer showed a certificate which was refused
            return new SchemeAttempt(null, isCertificateFailure(e));
        }
    }

    /**
     * @param stream     what the scheme produced, null if it produced nothing
     * @param conclusive whether trying the other scheme would still answer the question
     */
    private record SchemeAttempt(@Nullable InputStream stream, boolean conclusive) {
    }

    /**
     * A host which does not speak tls on that port answered nothing, and the other scheme of a network
     * path reference is the question to ask next. A host which showed a certificate the client refused
     * did answer, and the answer to a refused certificate is never the same resource in the clear.
     */
    @VisibleForTesting
    boolean isCertificateFailure(@NotNull Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof CertificateException || current instanceof SSLPeerUnverifiedException) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    @VisibleForTesting
    public InputStream resolveImpl(@NotNull URL url) {
        URL currentUrl = url;
        // the selector is asked once per hop, and the answer both decides and routes: what the check
        // let through cannot be routed anywhere else
        ProxySelector selector = ProxySelector.getDefault();
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            ProxyDecision proxy = decideProxy(selector, currentUrl);
            if (proxy.proxied() && !policy.isExplicitlyTrusted(currentUrl)) {
                // A proxy resolves the host name itself, so the vetted addresses would decide nothing.
                // Only a host the configuration trusts as such may be fetched that way.
                logger.warn(SKIPPED_RESOURCE + currentUrl + ": it would go through a proxy, which resolves the host name itself."
                        + " List the host in the allowed hosts property to fetch it anyway.");
                return null;
            }
            InetAddress[] addresses = policy.vetAddresses(currentUrl);
            if (addresses == null) {
                return null;
            }
            try (CloseableHttpClient client = createClient(currentUrl, addresses, proxy);
                 CloseableHttpResponse response = client.execute(new HttpGet(URI.create(currentUrl.toString())))) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HTTP_OK) {
                    return readContent(response, currentUrl);
                }
                if (!REDIRECT_STATUS_CODES.contains(statusCode)) {
                    return null;
                }
                Header location = response.getFirstHeader(HttpHeaders.LOCATION);
                if (location == null || StringUtils.isEmptyTrimmed(location.getValue())) {
                    return null;
                }
                currentUrl = URI.create(normalizeUrl(currentUrl.toString())).resolve(normalizeUrl(location.getValue().trim())).toURL();
            }
        }
        logger.warn("Failed to load resource: " + url + ", more than " + MAX_REDIRECTS + " redirects");
        return null;
    }

    /**
     * Builds a client for a single request. Its name resolution answers with the vetted addresses for the
     * host of that request, which binds the request to what {@link ResourceUrlPolicy#vetAddresses}
     * approved. The host name stays in the request, so the Host header and the TLS host name verification
     * keep working. Any other name, a proxy host as a rule, is left to the system resolver.
     */
    private CloseableHttpClient createClient(@NotNull URL url, @NotNull InetAddress[] addresses, @NotNull ProxyDecision proxy) {
        String pinnedHost = url.getHost() == null ? "" : stripBrackets(url.getHost());
        DnsResolver pinnedResolver = host -> pinnedHost.equalsIgnoreCase(host)
                ? addresses
                : SystemDefaultDnsResolver.INSTANCE.resolve(host);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                .setSocketTimeout(READ_TIMEOUT_MS)
                // redirects are followed by this class, so that the policy sees every single hop
                .setRedirectsEnabled(false)
                .build();
        return HttpClients.custom()
                // the replaced HttpURLConnection honoured http.proxyHost and friends, keep doing that.
                // Behind a proxy the socket goes to the proxy, so the proxy resolves the host name and
                // the addresses below only decide whether the request is made at all.
                .useSystemProperties()
                // the route is the answer the check already got, so the selector is asked no second time
                .setRoutePlanner(proxy.host() == null ? new DefaultRoutePlanner(null) : new DefaultProxyRoutePlanner(proxy.host()))
                .setDefaultRequestConfig(requestConfig)
                .setDnsResolver(pinnedResolver)
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableAuthCaching()
                .build();
    }

    /**
     * Reads at most {@link ResourceUrlPolicy#getMaxResourceBytes()} bytes. The content is fully read here
     * because the response is closed as soon as this method returns.
     */
    @SneakyThrows
    private InputStream readContent(@NotNull CloseableHttpResponse response, @NotNull URL url) {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }

        Header contentTypeHeader = entity.getContentType();
        String contentType = contentTypeHeader == null ? null : contentTypeHeader.getValue();
        if (!policy.isAllowedContentType(contentType)) {
            logger.warn(SKIPPED_RESOURCE + url + ": the content type '" + contentType + "' is not an image, a font or a stylesheet");
            return null;
        }

        long maxBytes = policy.getMaxResourceBytes();
        if (entity.getContentLength() > maxBytes) {
            logger.warn(SKIPPED_RESOURCE + url + ": it is larger than " + maxBytes + " bytes");
            return null;
        }

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = entity.getContent()) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                if (content.size() + read > maxBytes) {
                    logger.warn(SKIPPED_RESOURCE + url + ": it is larger than " + maxBytes + " bytes");
                    return null;
                }
                content.write(buffer, 0, read);
            }
        }

        byte[] bytes = content.toByteArray();
        if (policy.isSniffingRequired(contentType)) {
            // A missing or generic content type passes the header check, so the content decides. This
            // catches an internal service which answers with a document instead of an image.
            String sniffedType = MediaUtils.getMimeTypeUsingTikaByContent(url.toString(), bytes);
            if (policy.isRejectedSniffedType(sniffedType)) {
                logger.warn(SKIPPED_RESOURCE + url + ": its content is '" + sniffedType + "', not an image, a font or a stylesheet");
                return null;
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    private String ensureAbsoluteUrl(String url) {
        return url.startsWith("/") ? getBaseUrl() + url : url;
    }

    private static String stripBrackets(@NotNull String host) {
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    /**
     * Asks the selector once and keeps the answer. The request is routed by this answer and by nothing
     * else, so a selector which answers differently the next time it is asked cannot route a request the
     * check let through. Only an http proxy is a route: a socks one the jvm applies at the socket, after
     * the connection has been bound to a vetted address.
     *
     * @return what to do with the url, and where to route it if that is through a proxy
     */
    @VisibleForTesting
    ProxyDecision decideProxy(@Nullable ProxySelector selector, @NotNull URL url) {
        if (selector == null) {
            return new ProxyDecision(false, null);
        }
        try {
            return selector.select(URI.create(url.toString())).stream()
                    .filter(proxy -> proxy.type() == Proxy.Type.HTTP)
                    .findFirst()
                    .map(proxy -> new ProxyDecision(true, hostOf(proxy)))
                    .orElseGet(() -> new ProxyDecision(false, null));
        } catch (RuntimeException e) {
            // a selector which cannot answer is no reason to fetch anything past the check
            logger.warn("Cannot tell whether '" + url + "' goes through a proxy, it is treated as proxied", e);
            return new ProxyDecision(true, null);
        }
    }

    @Nullable
    private HttpHost hostOf(@NotNull Proxy proxy) {
        if (proxy.address() instanceof InetSocketAddress address) {
            return new HttpHost(address.getHostString(), address.getPort());
        }
        return null;
    }

    /**
     * @param proxied whether the url would be requested through an http proxy
     * @param host    the proxy to route to, null where there is nothing to route to or nothing was told
     */
    @VisibleForTesting
    record ProxyDecision(boolean proxied, @Nullable HttpHost host) {
    }

    private String getBaseUrl() {
        String baseUrl = System.getProperty("base.url").trim();
        return StringUtils.removeSuffix(baseUrl, "/");
    }

    // delegates, so that the policy check and the request itself normalize a url identically
    private String normalizeUrl(String urlStr) {
        return MediaUtils.normalizeUrl(urlStr);
    }
}
