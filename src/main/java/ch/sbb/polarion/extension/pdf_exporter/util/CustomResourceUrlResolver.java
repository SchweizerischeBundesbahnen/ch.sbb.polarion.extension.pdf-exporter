package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.*;

/**
 * Custom version of {@link com.polarion.alm.tracker.internal.url.GenericUrlResolver} with the redirect support.
 */
public class CustomResourceUrlResolver implements IUrlResolver {

    private static final int CONNECTION_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;

    public boolean canResolve(@NotNull String url) {
        return Stream.of("/", "http:", "https:").anyMatch(url::startsWith);
    }

    public InputStream resolve(@NotNull String urlStr) {
        try {
            URI uri = URI.create(normalizeUrl(ensureAbsoluteUrl(urlStr)));
            return resolveImpl(uri.toURL());
        } catch (Exception e) {
            Logger.getLogger(this).warn("Failed to load resource: " + urlStr, e);
        }
        return null;
    }

    @SneakyThrows
    @VisibleForTesting
    public InputStream resolveImpl(@NotNull URL url) {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HTTP_OK) {
            return connection.getInputStream();
        } else if (responseCode == HTTP_MOVED_PERM || responseCode == HTTP_MOVED_TEMP) {
            String location = connection.getHeaderField("Location");
            if (location != null && canResolve(location)) {
                return resolve(location);
            }
        }
        return null;
    }

    private String ensureAbsoluteUrl(String url) {
        return url.startsWith("/") ? getBaseUrl() + url : url;
    }

    private String getBaseUrl() {
        String baseUrl = System.getProperty("base.url").trim();
        return StringUtils.removeSuffix(baseUrl, "/");
    }

    private String normalizeUrl(String urlStr) {
        return urlStr.replace(" ", "%20").replace("%5F", "_");
    }
}
