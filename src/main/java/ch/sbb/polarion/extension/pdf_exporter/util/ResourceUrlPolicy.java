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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String CSS = "text/css";

    private static final List<String> ALLOWED_CONTENT_TYPE_PREFIXES = List.of(
            "image/", "font/", "application/font", "application/x-font", CSS,
            "application/octet-stream", "binary/octet-stream");

    // What the content itself may turn out to be. This is the allowed list once more, without the
    // generic types: those say nothing, and the question here is what the bytes are. Tika reports an
    // SVG as image/svg+xml, so nothing has to be said about xml for an image's sake.
    private static final List<String> ALLOWED_SNIFFED_TYPE_PREFIXES = List.of(
            "image/", "font/", "application/font", "application/x-font", CSS);

    // What a text looks like to a detector, and the two kinds of resource which are one. A stylesheet
    // reads as plain text and an SVG without a namespace does too, so the sender has to name those.
    private static final Set<String> TEXTUAL_CONTENT = Set.of("text/plain", "application/xml", "text/xml");
    private static final Set<String> TEXTUAL_RESOURCE_TYPES = Set.of(CSS, "image/svg+xml");

    private final Mode mode;
    private final Set<AllowedOrigin> allowedOrigins;
    private final String baseUrlHost;
    private final String baseUrlScheme;
    private final int baseUrlPort;
    private final long maxResourceBytes;

    public ResourceUrlPolicy(@NotNull Mode mode, @Nullable Collection<String> allowedOrigins, @Nullable String baseUrl, int maxSizeMB) {
        this.mode = mode;
        this.allowedOrigins = new HashSet<>();
        if (allowedOrigins != null) {
            allowedOrigins.stream()
                    .filter(origin -> origin != null && !origin.isBlank())
                    .map(AllowedOrigin::parse)
                    .filter(Objects::nonNull)
                    .forEach(this.allowedOrigins::add);
        }
        this.maxResourceBytes = (long) maxSizeMB * 1024 * 1024;

        URI baseUri = parseBaseUrl(baseUrl);
        this.baseUrlHost = baseUri == null || baseUri.getHost() == null ? null : baseUri.getHost().toLowerCase(Locale.ROOT);
        this.baseUrlPort = baseUri == null ? -1 : effectivePort(baseUri.getScheme(), baseUri.getPort());
        this.baseUrlScheme = baseUri != null && HTTPS.equalsIgnoreCase(baseUri.getScheme()) ? HTTPS : HTTP;
    }

    public static ResourceUrlPolicy getInstance() {
        PdfExporterExtensionConfiguration configuration = PdfExporterExtensionConfiguration.getInstance();
        String allowedOrigins = configuration.getExternalResourcesAllowedOrigins();
        int maxSizeMB = configuration.getExternalResourcesMaxSizeMB();
        return new ResourceUrlPolicy(
                Mode.parse(configuration.getExternalResourcesPolicy()),
                allowedOrigins == null ? List.of() : Arrays.asList(allowedOrigins.split(",")),
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
     * Judges the content of a resource against what its sender called it. The header alone leaves the
     * verdict to the sender, so the content has to agree with it: an image or a font has a shape, and
     * text does not, which is why a stylesheet and an SVG are believed only where the sender named one.
     *
     * @param declaredType the content type the sender reported, null when it reported none
     * @param sniffedType  the media type detected in the content, null when nothing was detected
     * @return true if the resource may not be used
     */
    public boolean isRejectedContent(@Nullable String declaredType, @Nullable String sniffedType) {
        boolean saidNothing = declaredType == null || declaredType.isBlank()
                || mediaType(declaredType).endsWith("/octet-stream");
        if (sniffedType == null) {
            // nothing was read out of the content, so only a sender which named a kind is believed
            return saidNothing;
        }
        String sniffed = mediaType(sniffedType);
        if (ALLOWED_SNIFFED_TYPE_PREFIXES.stream().anyMatch(sniffed::startsWith)) {
            return false;
        }
        return saidNothing || !TEXTUAL_CONTENT.contains(sniffed) || !TEXTUAL_RESOURCE_TYPES.contains(mediaType(declaredType));
    }

    private static String mediaType(@NotNull String contentType) {
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAllowed(@NotNull URL url) {
        return vetAddresses(url) != null;
    }

    /**
     * Tells whether the policy refuses this url whatever it resolves to: a scheme nothing may request,
     * a url without a host, or an origin the configuration does not name where nothing else may be
     * requested. What is left after that is the address of the host, and that question belongs to the
     * request itself, which binds to the answer it got. A caller which only decides whether to try at
     * all asks this one, so that a name only a proxy can resolve is not refused here.
     */
    public boolean isRefusedByOrigin(@NotNull URL url) {
        String scheme = schemeOf(url);
        if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
            return true;
        }
        String host = hostOf(url);
        if (host.isBlank()) {
            return true;
        }
        return mode == Mode.ALLOWLIST_ONLY && !isExempt(scheme, host, effectivePort(scheme, url.getPort()));
    }

    /**
     * @return the addresses the url may be requested from, null if the url may not be requested at all.
     * The caller connects to exactly these addresses, which is what makes the check binding: a second
     * name resolution cannot answer with an address this method did not see.
     */
    @Nullable
    public InetAddress[] vetAddresses(@NotNull URL url) {
        String protocol = schemeOf(url);
        if (!HTTP.equals(protocol) && !HTTPS.equals(protocol)) {
            return denyAddresses(url, "only http and https are supported");
        }

        String host = hostOf(url);
        if (host.isBlank()) {
            return denyAddresses(url, "no host");
        }
        int port = effectivePort(protocol, url.getPort());

        boolean exempt = isExempt(protocol, host, port);
        if (!exempt && mode == Mode.ALLOWLIST_ONLY) {
            return denyAddresses(url, "the origin is not in " + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_ORIGINS);
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
        String host = hostOf(url);
        String scheme = schemeOf(url);
        return !host.isBlank() && isExempt(scheme, host, effectivePort(scheme, url.getPort()));
    }

    @NotNull
    private static String schemeOf(@NotNull URL url) {
        String protocol = url.getProtocol();
        return protocol == null ? "" : protocol.toLowerCase(Locale.ROOT);
    }

    @NotNull
    private static String hostOf(@NotNull URL url) {
        String host = url.getHost();
        return host == null ? "" : stripBrackets(host.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isExempt(@NotNull String scheme, @NotNull String host, int port) {
        return isBaseUrlOrigin(host, port) || isAllowedOrigin(scheme, host, port) || mode == Mode.ALLOW_ALL;
    }

    /**
     * Tells whether a refusal of this url turned on its scheme. An allowed origin may name one,
     * {@code https://cdn.example.com} allows that spelling of the host and no other, so the same
     * reference under the other scheme is a question of its own.
     *
     * @return true when the url is not exempt as it stands, while the other scheme of it is
     */
    public boolean isRefusalSchemeSpecific(@NotNull URL url) {
        String host = hostOf(url);
        if (host.isBlank()) {
            return false;
        }
        String scheme = schemeOf(url);
        String otherScheme = HTTPS.equals(scheme) ? HTTP : HTTPS;
        return !isExempt(scheme, host, effectivePort(scheme, url.getPort()))
                && isExempt(otherScheme, host, effectivePort(otherScheme, url.getPort()));
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

    private boolean isAllowedOrigin(@NotNull String scheme, @NotNull String host, int port) {
        return allowedOrigins.stream().anyMatch(origin -> origin.matches(scheme, host, port));
    }

    /**
     * One entry of the allowed origins. What the entry does not write is not compared: a bare host
     * allows the resource under either scheme and on any port, {@code cdn.example.com:8443} on that
     * port under either scheme, {@code https://cdn.example.com} on the port https implies and under
     * https alone, and {@code https://cdn.example.com:8443} on exactly that origin.
     *
     * @param scheme the scheme the entry names, null where it names none
     * @param host   the host the entry names, never null
     * @param port   the port the entry names, -1 where it names none
     */
    private record AllowedOrigin(@Nullable String scheme, @NotNull String host, int port) {

        private static final Pattern ENTRY_PATTERN = Pattern.compile(
                "^(?:(?<scheme>[a-z][a-z\\d+.-]*)://)?(?<host>\\[[^\\]]+]|[^:/?#\\[\\]]+)(?::(?<port>\\d{1,5}))?$");

        @Nullable
        private static AllowedOrigin parse(@NotNull String entry) {
            Matcher matcher = ENTRY_PATTERN.matcher(entry.trim().toLowerCase(Locale.ROOT));
            if (!matcher.matches()) {
                return ignored(entry, "it names no origin of the form [scheme://]host[:port]");
            }
            String scheme = matcher.group("scheme");
            if (scheme != null && !HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
                return ignored(entry, "only http and https can be requested");
            }
            String port = matcher.group("port");
            int portNumber = port == null ? -1 : Integer.parseInt(port);
            if (portNumber > 65535) {
                return ignored(entry, port + " is no port");
            }
            // a scheme without a port names the port that scheme implies, which is what an origin is
            int effective = scheme != null && portNumber == -1 ? effectivePort(scheme, -1) : portNumber;
            return new AllowedOrigin(scheme, stripBrackets(matcher.group("host")), effective);
        }

        @Nullable
        private static AllowedOrigin ignored(@NotNull String entry, @NotNull String reason) {
            logger.warn("Ignored the entry '" + entry.trim() + "' of the property "
                    + PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_ORIGINS + ", " + reason);
            return null;
        }

        private boolean matches(@NotNull String scheme, @NotNull String host, int port) {
            return this.host.equals(host)
                    && (this.scheme == null || this.scheme.equals(scheme))
                    && (this.port == -1 || this.port == port);
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

        return !isPrivateOrLocalIpv4(first, second)
                && !isSpecialUseIpv4(first, second, third)
                && first < 240; // 240.0.0.0/4, reserved, includes the broadcast address
    }

    private static boolean isPrivateOrLocalIpv4(int first, int second) {
        return first == 0                                        // 0.0.0.0/8, this network
                || first == 10                                   // 10.0.0.0/8, private
                || first == 127                                  // 127.0.0.0/8, loopback
                || (first == 172 && second >= 16 && second <= 31) // 172.16.0.0/12, private
                || (first == 192 && second == 168)               // 192.168.0.0/16, private
                || (first == 169 && second == 254)               // 169.254.0.0/16, link local, holds the cloud metadata address
                || (first == 100 && second >= 64 && second <= 127); // 100.64.0.0/10, carrier grade NAT
    }

    /**
     * Ranges which are assigned but not globally routable. A deployment may point any of them at
     * something of its own, and none of them names a host on the internet, so none may be requested
     * on behalf of a document editor.
     */
    private static boolean isSpecialUseIpv4(int first, int second, int third) {
        return (first >= 224 && first <= 239)                    // 224.0.0.0/4, multicast
                || (first == 192 && second == 0 && third == 0)   // 192.0.0.0/24, protocol assignments
                || (first == 192 && second == 0 && third == 2)   // 192.0.2.0/24, documentation
                || (first == 192 && second == 88 && third == 99) // 192.88.99.0/24, 6to4 relay anycast
                || (first == 198 && (second == 18 || second == 19)) // 198.18.0.0/15, benchmarking
                || (first == 198 && second == 51 && third == 100) // 198.51.100.0/24, documentation
                || (first == 203 && second == 0 && third == 113); // 203.0.113.0/24, documentation
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
        return !isSpecialUseIpv6(bytes);
    }

    /**
     * Ranges which are assigned but not globally routable, the counterpart of the IPv4 list above. A
     * deployment may point any of them at something of its own, and none of them names a host on the
     * internet.
     */
    private static boolean isSpecialUseIpv6(byte[] bytes) {
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        int third = bytes[2] & 0xFF;

        if (first == 0x01 && second == 0x00) { // 100::/64, discard only
            return isZero(bytes, 2, 8);
        }
        if (first == 0x20 && second == 0x01 && third == 0x0d && (bytes[3] & 0xFF) == 0xb8) {
            return true; // 2001:db8::/32, documentation
        }
        if (first == 0x20 && second == 0x01) {
            // 2001::/23, the protocol assignments: Teredo, benchmarking, ORCHID and the rest of them
            return (third & 0xFE) == 0;
        }
        if (first == 0x3f && second == 0xff) { // 3fff::/20, documentation
            return (third & 0xF0) == 0;
        }
        return first == 0x5f && second == 0x00; // 5f00::/16, segment routing
    }

    private static boolean isZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
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

        /**
         * Reads the property. The value is one of the names above, written exactly as they are: a
         * setting which is nearly right is a setting whose author expects something else to happen.
         */
        public static Mode parse(@Nullable String value) {
            if (value != null && !value.isBlank()) {
                for (Mode mode : values()) {
                    if (mode.name().equals(value.trim())) {
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
