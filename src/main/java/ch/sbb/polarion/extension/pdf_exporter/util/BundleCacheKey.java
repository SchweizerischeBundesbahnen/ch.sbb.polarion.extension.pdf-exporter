package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * The query value that busts the browser cache of the app's fixed-name modules.
 * <p>
 * The server-side surfaces import {@code bulk-widget.js}, {@code export-popup.js} and {@code side-panel.js} by
 * a fixed URL, since they cannot know the hashed names Vite gives the rest of the bundle. The bundle version
 * alone does not change between two builds of one version, a SNAPSHOT or a rebuilt release, and the browser
 * then keeps running the module of the earlier build. The build timestamp changes with every build; the version
 * stays in front of it so that the URL still says which release it is.
 */
@UtilityClass
public class BundleCacheKey {

    /** The value for a jar without a manifest version, which is a build that did not come from Maven. */
    static final String UNKNOWN_VERSION = "0";

    /** {@code 13.7.1-202609221423}, or the version alone where the manifest carries no build timestamp. */
    public static @NotNull String get() {
        Version version = VersionUtils.getVersion();
        String bundleVersion = version.getBundleVersion() == null ? UNKNOWN_VERSION : version.getBundleVersion();
        String timestamp = version.getBundleBuildTimestamp();
        return timestamp == null ? bundleVersion : bundleVersion + "-" + timestamp.replaceAll("\\D", "");
    }
}
