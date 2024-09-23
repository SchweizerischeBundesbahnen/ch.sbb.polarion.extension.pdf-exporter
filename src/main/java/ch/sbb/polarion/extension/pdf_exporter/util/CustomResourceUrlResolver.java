package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.*;

/**
 * Custom version of {@link com.polarion.alm.tracker.internal.url.GenericUrlResolver} with the redirect support.
 */
public class CustomResourceUrlResolver implements IUrlResolver {

    private static final int CONNECTION_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 3_000;

    public boolean canResolve(@NotNull String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public InputStream resolve(@NotNull String urlStr) {
        try {
            URL url = new URL(normalizeUrl(urlStr));
            return resolveImpl(url);
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

    private String normalizeUrl(String urlStr) {
        return urlStr.replace(" ", "%20").replace("%5F", "_");
    }
}
