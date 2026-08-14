package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.*;

/**
 * Custom version of {@link com.polarion.alm.tracker.internal.url.GenericUrlResolver} with the redirect support.
 * <p>
 * Every request passes {@link ResourceUrlPolicy}, which keeps a document editor from making the server
 * fetch an arbitrary address and from getting the response body back in the exported document.
 * </p>
 */
public class CustomResourceUrlResolver implements IUrlResolver {

    private static final Logger logger = Logger.getLogger(CustomResourceUrlResolver.class);

    private static final int CONNECTION_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;
    private static final int MAX_REDIRECTS = 5;
    private static final int BUFFER_SIZE = 8192;

    private final ResourceUrlPolicy policy;

    public CustomResourceUrlResolver() {
        this(ResourceUrlPolicy.getInstance());
    }

    @VisibleForTesting
    public CustomResourceUrlResolver(@NotNull ResourceUrlPolicy policy) {
        this.policy = policy;
    }

    public boolean canResolve(@NotNull String url) {
        return Stream.of("/", "http:", "https:").anyMatch(url::startsWith);
    }

    public InputStream resolve(@NotNull String urlStr) {
        try {
            URL url = URI.create(normalizeUrl(ensureAbsoluteUrl(urlStr))).toURL();
            return resolveImpl(url);
        } catch (Exception e) {
            logger.warn("Failed to load resource: " + urlStr, e);
        }
        return null;
    }

    @SneakyThrows
    @VisibleForTesting
    public InputStream resolveImpl(@NotNull URL url) {
        URL currentUrl = url;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            if (!policy.isAllowed(currentUrl)) {
                return null;
            }
            HttpURLConnection connection = openConnection(currentUrl);
            try {
                int responseCode = connection.getResponseCode();
                if (responseCode == HTTP_OK) {
                    return readContent(connection, currentUrl);
                }
                if (responseCode != HTTP_MOVED_PERM && responseCode != HTTP_MOVED_TEMP) {
                    return null;
                }
                String location = connection.getHeaderField("Location");
                if (StringUtils.isEmptyTrimmed(location)) {
                    return null;
                }
                currentUrl = URI.create(normalizeUrl(currentUrl.toString())).resolve(normalizeUrl(location.trim())).toURL();
            } finally {
                connection.disconnect();
            }
        }
        logger.warn("Failed to load resource: " + url + ", more than " + MAX_REDIRECTS + " redirects");
        return null;
    }

    @SneakyThrows
    private HttpURLConnection openConnection(@NotNull URL url) {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        // redirects are followed by this class, so that the policy sees every single hop
        connection.setInstanceFollowRedirects(false);
        connection.connect();
        return connection;
    }

    /**
     * Reads at most {@link ResourceUrlPolicy#getMaxResourceBytes()} bytes. The content is fully read here
     * because the connection is closed as soon as this method returns.
     */
    @SneakyThrows
    private InputStream readContent(@NotNull HttpURLConnection connection, @NotNull URL url) {
        String contentType = connection.getContentType();
        if (!policy.isAllowedContentType(contentType)) {
            logger.warn("Skipped resource " + url + ": the content type '" + contentType + "' is not an image, a font or a stylesheet");
            return null;
        }

        long maxBytes = policy.getMaxResourceBytes();
        if (connection.getContentLengthLong() > maxBytes) {
            logger.warn("Skipped resource " + url + ": it is larger than " + maxBytes + " bytes");
            return null;
        }

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = connection.getInputStream()) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                if (content.size() + read > maxBytes) {
                    logger.warn("Skipped resource " + url + ": it is larger than " + maxBytes + " bytes");
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
                logger.warn("Skipped resource " + url + ": its content is '" + sniffedType + "', not an image, a font or a stylesheet");
                return null;
            }
        }
        return new ByteArrayInputStream(bytes);
    }

    private String ensureAbsoluteUrl(String url) {
        return url.startsWith("/") ? getBaseUrl() + url : url;
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
