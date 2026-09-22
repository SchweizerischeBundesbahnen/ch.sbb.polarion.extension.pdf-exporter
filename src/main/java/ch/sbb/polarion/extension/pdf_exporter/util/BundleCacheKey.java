package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The query value that busts the browser cache of the app's fixed-name modules.
 * <p>
 * The server-side surfaces import {@code bulk-widget.js}, {@code export-popup.js} and {@code side-panel.js} by
 * a fixed URL, since they cannot know the hashed names Vite gives the rest of the bundle. The bundle version
 * alone does not change between two builds of one version, a SNAPSHOT or a rebuilt release, and the browser
 * then keeps running the module of the earlier build. The build timestamp would change, but only to the minute.
 * <p>
 * So the key is a hash of the module itself. A fixed-name entry imports the hashed chunks by their names, so it
 * changes whenever any code it runs changes, and a rebuild that changes nothing keeps the browser's copy. The
 * version stays in front of the hash so that the URL still says which release it is.
 */
@UtilityClass
public class BundleCacheKey {

    private static final Logger logger = Logger.getLogger(BundleCacheKey.class);

    /** The value for a jar without a manifest version, which is a build that did not come from Maven. */
    static final String UNKNOWN_VERSION = "0";

    /** How much of the SHA-256 goes into the key: 48 bits, which no two builds of one version share by chance. */
    static final int HASH_LENGTH = 12;

    /** The hashes, per module. The jar does not change while it is deployed, so each module is read once. */
    private static final Map<String, String> HASHES = new ConcurrentHashMap<>();

    /**
     * {@code 13.7.1-3f9c2a7b1e04} for the module at {@code modulePath} in the jar, e.g.
     * {@code webapp/pdf-exporter-app/app/assets/bulk-widget.js}, or the version alone where the module cannot
     * be read.
     */
    public static @NotNull String forModule(@NotNull String modulePath) {
        String bundleVersion = VersionUtils.getVersion().getBundleVersion();
        String version = bundleVersion == null ? UNKNOWN_VERSION : bundleVersion;
        String hash = HASHES.computeIfAbsent(modulePath, path -> {
            String computed = hashOf(path);
            // An empty value is kept too, so that a missing module is not looked up on every render
            return computed == null ? "" : computed;
        });
        return hash.isEmpty() ? version : version + "-" + hash;
    }

    private static @Nullable String hashOf(@NotNull String modulePath) {
        try (InputStream module = BundleCacheKey.class.getClassLoader().getResourceAsStream(modulePath)) {
            if (module == null) {
                // Keyed by the version alone, a rebuild of one version would be served from the browser cache
                // again. Once per module: the result is kept.
                logger.warn("Module %s is not in the bundle; its cache key falls back to the version alone"
                        .formatted(modulePath));
                return null;
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(module.readAllBytes());
            return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.warn("Module %s could not be hashed; its cache key falls back to the version alone"
                    .formatted(modulePath), e);
            return null;
        }
    }
}
