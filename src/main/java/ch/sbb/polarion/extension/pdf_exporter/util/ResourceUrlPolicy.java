package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides which resource URLs the exporter may request while inlining images, fonts and stylesheets.
 * <p>
 * A document editor controls those URLs. Without a policy the Polarion server would fetch any address
 * on behalf of the editor and the response body would end up in the exported document, which is a
 * server side request forgery with full read access to the server's network.
 * </p>
 */
public class ResourceUrlPolicy {

    private static final Logger logger = Logger.getLogger(ResourceUrlPolicy.class);

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String ADDRESS_CACHE_TTL_PROPERTY = "networkaddress.cache.ttl";
    private static final AtomicBoolean ADDRESS_CACHE_WARNED = new AtomicBoolean();

    private static final List<String> ALLOWED_CONTENT_TYPE_PREFIXES = List.of(
            "image/", "font/", "application/font", "application/x-font", "text/css",
            "application/octet-stream", "binary/octet-stream");

    // XML is not listed on purpose: Tika reports a plain SVG as application/xml often enough,
    // and rejecting it would drop legitimate images.
    private static final Set<String> REJECTED_SNIFFED_TYPES = Set.of(
            "text/html", "application/xhtml+xml", "application/json");

    private final Mode mode;
    private final Set<String> allowedHosts;
    private final String baseUrlHost;
    private final int baseUrlPort;
    private final long maxResourceBytes;

    public ResourceUrlPolicy(@NotNull Mode mode, @Nullable Collection<String> allowedHosts, @Nullable String baseUrl, int maxSizeMB) {
        this.mode = mode;
        this.allowedHosts = new HashSet<>();
        if (allowedHosts != null) {
            allowedHosts.stream()
                    .filter(host -> !StringUtils.isEmptyTrimmed(host))
                    .map(host -> host.trim().toLowerCase(Locale.ROOT))
                    .forEach(this.allowedHosts::add);
        }
        this.maxResourceBytes = (long) maxSizeMB * 1024 * 1024;

        URI baseUri = parseBaseUrl(baseUrl);
        this.baseUrlHost = baseUri == null || baseUri.getHost() == null ? null : baseUri.getHost().toLowerCase(Locale.ROOT);
        this.baseUrlPort = baseUri == null ? -1 : effectivePort(baseUri.getScheme(), baseUri.getPort());
    }

    public static ResourceUrlPolicy getInstance() {
        PdfExporterExtensionConfiguration configuration = PdfExporterExtensionConfiguration.getInstance();
        String allowedHosts = configuration.getExternalResourcesAllowedHosts();
        int maxSizeMB = configuration.getExternalResourcesMaxSizeMB();
        return new ResourceUrlPolicy(
                Mode.parse(configuration.getExternalResourcesPolicy()),
                allowedHosts == null ? List.of() : Arrays.asList(allowedHosts.split(",")),
                System.getProperty(PolarionProperties.BASE_URL),
                maxSizeMB > 0 ? maxSizeMB : PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_MAX_SIZE_MB_DEFAULT_VALUE);
    }

    /**
     * @return maximal size in bytes a single fetched resource may reach
     */
    public long getMaxResourceBytes() {
        return maxResourceBytes;
    }

    /**
     * Checks the content type a remote host reports for a resource. An internal service answers with
     * its own media type, JSON as a rule, so this check stops most of the responses a forged request
     * could return. An absent content type is accepted, the content is sniffed later anyway.
     */
    public boolean isAllowedContentType(@Nullable String contentType) {
        if (StringUtils.isEmptyTrimmed(contentType)) {
            return true;
        }
        String mediaType = mediaType(contentType);
        return ALLOWED_CONTENT_TYPE_PREFIXES.stream().anyMatch(mediaType::startsWith);
    }

    /**
     * A missing or generic content type tells nothing, so the content itself has to be looked at.
     */
    public boolean isSniffingRequired(@Nullable String contentType) {
        return StringUtils.isEmptyTrimmed(contentType) || mediaType(contentType).endsWith("/octet-stream");
    }

    /**
     * @param sniffedType media type detected in the content, null when nothing was detected
     * @return true if the content is a document, which no image, font or stylesheet ever is
     */
    public boolean isRejectedSniffedType(@Nullable String sniffedType) {
        return sniffedType != null && REJECTED_SNIFFED_TYPES.contains(mediaType(sniffedType));
    }

    private static String mediaType(@NotNull String contentType) {
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAllowed(@NotNull URL url) {
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!HTTP.equals(protocol) && !HTTPS.equals(protocol)) {
            return deny(url, "only http and https are supported");
        }

        String host = url.getHost();
        if (StringUtils.isEmptyTrimmed(host)) {
            return deny(url, "no host");
        }
        host = host.toLowerCase(Locale.ROOT);
        int port = effectivePort(protocol, url.getPort());

        if (isBaseUrlOrigin(host, port) || isExplicitlyAllowed(host, port)) {
            return true;
        }

        return switch (mode) {
            case ALLOW_ALL -> true;
            case ALLOWLIST_ONLY -> deny(url, "the host is not in " + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_HOSTS);
            case BLOCK_INTERNAL -> hasOnlyPublicAddresses(url, host);
        };
    }

    private boolean isBaseUrlOrigin(@NotNull String host, int port) {
        return host.equals(baseUrlHost) && port == baseUrlPort;
    }

    private boolean isExplicitlyAllowed(@NotNull String host, int port) {
        return allowedHosts.contains(host) || allowedHosts.contains(host + ":" + port);
    }

    /**
     * Every address the host resolves to must be public. A host resolving to both a public and a
     * private address is rejected, otherwise the private one stays reachable.
     * <p>
     * The connection resolves the same name a second time, so a DNS rebinding attack would need the
     * answer to change between the two calls. It cannot: this call fills the JVM address cache, and
     * the connection reads it from there. The cache holds a positive answer for
     * {@code networkaddress.cache.ttl} seconds, 30 by default. An installation which sets that
     * property to 0 loses this protection, therefore {@link #warnAboutDisabledAddressCache()} logs it.
     * Binding the socket to the vetted address instead is not possible here: {@code HttpURLConnection}
     * refuses a {@code Host} header, and a literal address would break TLS host name verification.
     * </p>
     */
    private boolean hasOnlyPublicAddresses(@NotNull URL url, @NotNull String host) {
        warnAboutDisabledAddressCache();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return deny(url, "the host resolves to no address");
            }
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    return deny(url, "the host resolves to the non public address " + address.getHostAddress());
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return deny(url, "the host cannot be resolved");
        }
    }

    @VisibleForTesting
    static boolean isPublicAddress(@NotNull InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return bytes.length == 4 ? isPublicIpv4(bytes) : isPublicIpv6(bytes);
    }

    @SuppressWarnings("java:S3776") // the ranges read better as a flat list of guards
    private static boolean isPublicIpv4(byte[] bytes) {
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        int third = bytes[2] & 0xFF;

        if (first == 0) { // 0.0.0.0/8, "this network"
            return false;
        }
        if (first == 10 || first == 127) { // 10.0.0.0/8 private, 127.0.0.0/8 loopback
            return false;
        }
        if (first == 172 && second >= 16 && second <= 31) { // 172.16.0.0/12, private
            return false;
        }
        if (first == 192 && second == 168) { // 192.168.0.0/16, private
            return false;
        }
        if (first == 169 && second == 254) { // 169.254.0.0/16, link local, holds the cloud metadata address
            return false;
        }
        if (first >= 224 && first <= 239) { // 224.0.0.0/4, multicast
            return false;
        }
        if (first == 100 && second >= 64 && second <= 127) { // 100.64.0.0/10, carrier grade NAT
            return false;
        }
        if (first == 192 && second == 0 && third == 0) { // 192.0.0.0/24, IETF protocol assignments
            return false;
        }
        if (first == 198 && (second == 18 || second == 19)) { // 198.18.0.0/15, benchmarking
            return false;
        }
        return first < 240; // 240.0.0.0/4, reserved, includes the broadcast address
    }

    private static boolean isPublicIpv6(byte[] bytes) {
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;

        if ((first & 0xFE) == 0xFC) { // fc00::/7, unique local
            return false;
        }
        if (isIpv4Embedded(bytes)) { // ::ffff:0:0/96 and ::/96, an IPv4 address in IPv6 form
            return isPublicIpv4(Arrays.copyOfRange(bytes, 12, 16));
        }
        if (first == 0x20 && second == 0x02) { // 2002::/16, 6to4 carries the IPv4 address in the prefix
            return isPublicIpv4(Arrays.copyOfRange(bytes, 2, 6));
        }
        return first != 0x20 || second != 0x01 || bytes[2] != 0 || bytes[3] != 0; // 2001:0::/32, Teredo
    }

    private static boolean isIpv4Embedded(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        int eleventh = bytes[10] & 0xFF;
        int twelfth = bytes[11] & 0xFF;
        return (eleventh == 0 && twelfth == 0) || (eleventh == 0xFF && twelfth == 0xFF);
    }

    /**
     * Logs once if the JVM address cache is off, because the policy relies on it, see
     * {@link #hasOnlyPublicAddresses}.
     */
    @VisibleForTesting
    static void warnAboutDisabledAddressCache() {
        if (ADDRESS_CACHE_WARNED.compareAndSet(false, true) && "0".equals(Security.getProperty(ADDRESS_CACHE_TTL_PROPERTY))) {
            logger.warn("The java security property '" + ADDRESS_CACHE_TTL_PROPERTY + "' is 0, which turns the JVM address cache off."
                    + " A host name can then resolve to a different address for the check and for the request itself."
                    + " Set it to a positive value, or list the hosts a document may load resources from.");
        }
    }

    private static boolean deny(@NotNull URL url, @NotNull String reason) {
        logger.warn("Blocked the request to '" + url + "': " + reason
                + ". See the '" + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY + "' property.");
        return false;
    }

    @Nullable
    private static URI parseBaseUrl(@Nullable String baseUrl) {
        if (StringUtils.isEmptyTrimmed(baseUrl)) {
            return null;
        }
        try {
            return URI.create(baseUrl.trim());
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot parse the Polarion base url '" + baseUrl + "'");
            return null;
        }
    }

    private static int effectivePort(@Nullable String scheme, int port) {
        if (port != -1) {
            return port;
        }
        return HTTPS.equalsIgnoreCase(scheme) ? 443 : 80;
    }

    public enum Mode {
        /**
         * Requests to loopback, private, link local and other non public addresses are rejected.
         */
        BLOCK_INTERNAL,
        /**
         * Only the Polarion base url and the explicitly allowed hosts may be requested.
         */
        ALLOWLIST_ONLY,
        /**
         * No restriction. The behavior before the policy existed, it exposes the server's network.
         */
        ALLOW_ALL;

        public static Mode parse(@Nullable String value) {
            if (!StringUtils.isEmptyTrimmed(value)) {
                String normalized = value.trim().replace("_", "").replace("-", "");
                for (Mode mode : values()) {
                    if (mode.name().replace("_", "").equalsIgnoreCase(normalized)) {
                        return mode;
                    }
                }
                logger.warn("Unknown value '" + value + "' of the property "
                        + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY + ", falling back to " + BLOCK_INTERNAL);
            }
            return BLOCK_INTERNAL;
        }
    }
}
