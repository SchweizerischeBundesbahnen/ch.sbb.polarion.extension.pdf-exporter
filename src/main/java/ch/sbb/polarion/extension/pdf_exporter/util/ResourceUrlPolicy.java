package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private final String baseUrlScheme;
    private final int baseUrlPort;
    private final long maxResourceBytes;

    public ResourceUrlPolicy(@NotNull Mode mode, @Nullable Collection<String> allowedHosts, @Nullable String baseUrl, int maxSizeMB) {
        this.mode = mode;
        this.allowedHosts = new HashSet<>();
        if (allowedHosts != null) {
            allowedHosts.stream()
                    .filter(host -> host != null && !host.isBlank())
                    .map(host -> host.trim().toLowerCase(Locale.ROOT))
                    .forEach(this.allowedHosts::add);
        }
        this.maxResourceBytes = (long) maxSizeMB * 1024 * 1024;

        URI baseUri = parseBaseUrl(baseUrl);
        this.baseUrlHost = baseUri == null || baseUri.getHost() == null ? null : baseUri.getHost().toLowerCase(Locale.ROOT);
        this.baseUrlPort = baseUri == null ? -1 : effectivePort(baseUri.getScheme(), baseUri.getPort());
        this.baseUrlScheme = baseUri != null && HTTPS.equalsIgnoreCase(baseUri.getScheme()) ? HTTPS : HTTP;
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
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        String mediaType = mediaType(contentType);
        return ALLOWED_CONTENT_TYPE_PREFIXES.stream().anyMatch(mediaType::startsWith);
    }

    /**
     * A missing or generic content type tells nothing, so the content itself has to be looked at.
     */
    public boolean isSniffingRequired(@Nullable String contentType) {
        return contentType == null || contentType.isBlank() || mediaType(contentType).endsWith("/octet-stream");
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
        return vetAddresses(url) != null;
    }

    /**
     * @return the addresses the url may be requested from, null if the url may not be requested at all.
     * The caller connects to exactly these addresses, which is what makes the check binding: a second
     * name resolution cannot answer with an address this method did not see.
     */
    @Nullable
    public InetAddress[] vetAddresses(@NotNull URL url) {
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!HTTP.equals(protocol) && !HTTPS.equals(protocol)) {
            return denyAddresses(url, "only http and https are supported");
        }

        String rawHost = url.getHost();
        if (rawHost == null || rawHost.isBlank()) {
            return denyAddresses(url, "no host");
        }
        String host = stripBrackets(rawHost.toLowerCase(Locale.ROOT));
        int port = effectivePort(protocol, url.getPort());

        boolean exempt = isExempt(host, port);
        if (!exempt && mode == Mode.ALLOWLIST_ONLY) {
            return denyAddresses(url, "the host is not in " + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_HOSTS);
        }

        return resolveAddresses(url, host, exempt);
    }

    /**
     * Unless the host is exempt, every address it resolves to must be public. A host resolving to both a
     * public and a private address is rejected, otherwise the private one stays reachable.
     */
    @Nullable
    private InetAddress[] resolveAddresses(@NotNull URL url, @NotNull String host, boolean exempt) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return denyAddresses(url, "the host resolves to no address");
            }
            for (InetAddress address : addresses) {
                if (!exempt && !isPublicAddress(address)) {
                    return denyAddresses(url, "the host resolves to the non public address " + address.getHostAddress());
                }
            }
            return addresses;
        } catch (UnknownHostException e) {
            return denyAddresses(url, "the host cannot be resolved");
        }
    }

    /**
     * Tells whether the configuration trusts the url as such, rather than its addresses: the Polarion
     * server itself, a host the administrator listed, or any host when the policy is off. Only such a
     * url may be requested through a proxy, where the connection cannot be pinned to a checked address.
     */
    public boolean isExplicitlyTrusted(@NotNull URL url) {
        String rawHost = url.getHost();
        if (rawHost == null || rawHost.isBlank()) {
            return false;
        }
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        return isExempt(stripBrackets(rawHost.toLowerCase(Locale.ROOT)), effectivePort(protocol, url.getPort()));
    }

    private boolean isExempt(@NotNull String host, int port) {
        return isBaseUrlOrigin(host, port) || isExplicitlyAllowed(host, port) || mode == Mode.ALLOW_ALL;
    }

    /**
     * @return the scheme a network path reference like {@code //host/path} gets, taken from the Polarion
     * base url the conversion service reads the document with
     */
    @NotNull
    public String getBaseUrlScheme() {
        return baseUrlScheme;
    }

    private static String stripBrackets(@NotNull String host) {
        return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
    }

    private boolean isBaseUrlOrigin(@NotNull String host, int port) {
        return host.equals(baseUrlHost) && port == baseUrlPort;
    }

    private boolean isExplicitlyAllowed(@NotNull String host, int port) {
        return allowedHosts.contains(host) || allowedHosts.contains(host + ":" + port);
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
        if (isNat64WellKnown(bytes)) { // 64:ff9b::/96, DNS64 answers with it and NAT64 forwards to the IPv4 address
            return isPublicIpv4(Arrays.copyOfRange(bytes, 12, 16));
        }
        if (isNat64LocalUse(bytes)) { // 64:ff9b:1::/48, the IPv4 address can sit at several offsets in there
            return false;
        }
        if (first == 0x20 && second == 0x02) { // 2002::/16, 6to4 carries the IPv4 address in the prefix
            return isPublicIpv4(Arrays.copyOfRange(bytes, 2, 6));
        }
        return first != 0x20 || second != 0x01 || bytes[2] != 0 || bytes[3] != 0; // 2001:0::/32, Teredo
    }

    private static boolean isNat64WellKnown(byte[] bytes) {
        if (bytes[0] != 0 || (bytes[1] & 0xFF) != 0x64 || (bytes[2] & 0xFF) != 0xFF || (bytes[3] & 0xFF) != 0x9B) {
            return false;
        }
        for (int i = 4; i < 12; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNat64LocalUse(byte[] bytes) {
        return bytes[0] == 0 && (bytes[1] & 0xFF) == 0x64 && (bytes[2] & 0xFF) == 0xFF && (bytes[3] & 0xFF) == 0x9B
                && bytes[4] == 0 && (bytes[5] & 0xFF) == 0x01;
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

    @Nullable
    private static InetAddress[] denyAddresses(@NotNull URL url, @NotNull String reason) {
        logger.warn("Blocked the request to '" + url + "': " + reason
                + ". See the '" + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY + "' property.");
        return null;
    }

    @Nullable
    private static URI parseBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
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
            if (value != null && !value.isBlank()) {
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
