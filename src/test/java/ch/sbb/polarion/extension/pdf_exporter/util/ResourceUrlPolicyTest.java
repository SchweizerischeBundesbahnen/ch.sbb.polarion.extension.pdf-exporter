package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.util.ResourceUrlPolicy.Mode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ResourceUrlPolicyTest {

    private static final String BASE_URL = "http://localhost:80/polarion";

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }

    private static ResourceUrlPolicy policy(Mode mode, List<String> allowedOrigins) {
        return new ResourceUrlPolicy(mode, allowedOrigins, BASE_URL, 16);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:8080/secret",
            "http://127.5.6.7/secret",
            "http://[::1]/secret",
            "http://10.0.0.5/secret",
            "http://172.16.0.5/secret",
            "http://192.168.1.5/secret",
            "http://169.254.169.254/latest/meta-data/",
            "http://[fd00::1]/secret",
            "http://[fe80::1]/secret",
            "http://100.64.0.1/secret",
            "http://192.0.0.1/secret",
            "http://198.18.0.1/secret",
            // assigned but not globally routable: a deployment may point any of them at its own service
            "http://192.0.2.1/secret",
            "http://198.51.100.1/secret",
            "http://203.0.113.1/secret",
            "http://192.88.99.1/secret",
            "http://240.0.0.1/secret",
            "http://0.0.0.0/secret",
            "http://[::ffff:127.0.0.1]/secret",
            "http://[2002:7f00:0001::]/secret",
            "http://[2002:0a00:0005::]/secret",
            "http://[2002:a9fe:a9fe::]/secret",
            "http://[2001:0:1::1]/secret",
            "http://224.0.0.1/secret"
    })
    void blocksNonPublicAddresses(String value) {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://8.8.8.8/img.png",
            "https://93.184.216.34/img.png",
            "http://[2606:4700:4700::1111]/img.png",
            "http://[2002:0808:0808::]/img.png"
    })
    void allowsPublicAddresses(String value) {
        assertTrue(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://8.8.8.8/img.png", "file:///etc/passwd", "jar:http://8.8.8.8/a.jar!/img.png"})
    void blocksForeignSchemes(String value) {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url(value)));
    }

    @Test
    void blocksUnresolvableHost() {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url("http://unresolvable-host.invalid/img.png")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://[2002:ac10:0005::]/secret",  // 6to4 of 172.16.0.5
            "http://[2002:c0a8:0105::]/secret",  // 6to4 of 192.168.1.5
            "http://[2002:e000:0001::]/secret",  // 6to4 of 224.0.0.1
            "http://[2002:6440:0001::]/secret",  // 6to4 of 100.64.0.1
            "http://[2002:c000:0001::]/secret",  // 6to4 of 192.0.0.1
            "http://[2002:c612:0001::]/secret",  // 6to4 of 198.18.0.1
            "http://[2002:c613:0001::]/secret",  // 6to4 of 198.19.0.1
            "http://[2002:0000:0001::]/secret",  // 6to4 of 0.0.0.1
            "http://[2002:f000:0001::]/secret",  // 6to4 of 240.0.0.1
            "http://[64:ff9b::a9fe:a9fe]/secret", // NAT64 well known prefix carrying 169.254.169.254
            "http://[64:ff9b:1::1]/secret"       // NAT64 local use prefix, the address can sit anywhere in it
    })
    void blocksNonPublicAddressesEmbeddedInIpv6(String value) {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://[2002:ac0f:0001::]/img.png",  // 6to4 of 172.15.0.1, just below the private range
            "http://[2002:ac20:0001::]/img.png",  // 6to4 of 172.32.0.1, just above it
            "http://[2002:a9fd:0001::]/img.png",  // 6to4 of 169.253.0.1, not link local
            "http://[2002:643f:0001::]/img.png",  // 6to4 of 100.63.0.1, below the shared range
            "http://[2002:6480:0001::]/img.png",  // 6to4 of 100.128.0.1, above it
            "http://[2002:c000:0101::]/img.png",  // 6to4 of 192.0.1.1, outside 192.0.0.0/24
            "http://[2002:c001:0001::]/img.png",  // 6to4 of 192.1.0.1
            "http://[2002:c611:0001::]/img.png",  // 6to4 of 198.17.0.1, below the benchmark range
            "http://[2002:c614:0001::]/img.png",  // 6to4 of 198.20.0.1, above it
            "http://[2001:0100::1]/img.png",      // not Teredo, the third byte is set
            "http://[2001:0001::1]/img.png",      // not Teredo, the fourth byte is set
            "http://[2003::1]/img.png",           // not Teredo, the second byte differs
            "http://[64:ff9b::808:808]/img.png",  // NAT64 well known prefix carrying 8.8.8.8
            "http://[64:ff9c::1]/img.png"         // next to the NAT64 prefix, not in it
    })
    void allowsPublicAddressesNextToTheBlockedRanges(String value) {
        assertTrue(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url(value)));
    }

    @Test
    @SneakyThrows
    void classifiesIpv4EmbeddedInIpv6() {
        assertFalse(ResourceUrlPolicy.isPublicAddress(ipv6(compatible(10, 0, 0, 5))));
        assertTrue(ResourceUrlPolicy.isPublicAddress(ipv6(compatible(8, 8, 8, 8))));
        assertFalse(ResourceUrlPolicy.isPublicAddress(ipv6(mapped(127, 0, 0, 1))));
        assertTrue(ResourceUrlPolicy.isPublicAddress(ipv6(mapped(8, 8, 8, 8))));
        // the tenth byte is set, so nothing is embedded and the address stays a plain public IPv6 one
        byte[] notEmbedded = compatible(10, 0, 0, 5);
        notEmbedded[9] = 1;
        assertTrue(ResourceUrlPolicy.isPublicAddress(ipv6(notEmbedded)));

        // only 0x0000 and 0xFFFF in the eleventh and twelfth byte mark an embedded IPv4 address
        byte[] halfZero = compatible(10, 0, 0, 5);
        halfZero[11] = 5;
        assertTrue(ResourceUrlPolicy.isPublicAddress(ipv6(halfZero)));
        byte[] halfMapped = mapped(10, 0, 0, 5);
        halfMapped[11] = 0;
        assertTrue(ResourceUrlPolicy.isPublicAddress(ipv6(halfMapped)));
    }

    @SneakyThrows
    private static InetAddress ipv6(byte[] address) {
        return Inet6Address.getByAddress(null, address, 0);
    }

    private static byte[] compatible(int a, int b, int c, int d) {
        return new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) a, (byte) b, (byte) c, (byte) d};
    }

    private static byte[] mapped(int a, int b, int c, int d) {
        return new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, (byte) a, (byte) b, (byte) c, (byte) d};
    }

    @Test
    void blocksUrlWithoutHost() {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowed(url("http:///img.png")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "not-a-url", "http://[unterminated"})
    void survivesAnUnusableBaseUrlValue(String baseUrl) {
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, Arrays.asList("8.8.4.4", null), baseUrl, 16);
        assertTrue(policy.isAllowed(url("https://8.8.4.4/img.png")));
        assertFalse(policy.isAllowed(url("http://10.0.0.5/img.png")));
    }

    @Test
    void allowsPolarionBaseUrlItself() {
        ResourceUrlPolicy policy = policy(Mode.ALLOWLIST_ONLY, List.of());
        assertTrue(policy.isAllowed(url("http://localhost/polarion/some-resource.png")));
        assertTrue(policy.isAllowed(url("http://LOCALHOST:80/polarion/some-resource.png")));
        assertFalse(policy.isAllowed(url("http://localhost:8080/polarion/some-resource.png")));
    }

    @Test
    void tellsWhichUrlsTheConfigurationTrustsAsSuch() {
        ResourceUrlPolicy policy = policy(Mode.BLOCK_INTERNAL, List.of("8.8.4.4"));
        assertTrue(policy.isExplicitlyTrusted(url("https://8.8.4.4/img.png")));
        assertTrue(policy.isExplicitlyTrusted(url("http://localhost/polarion/img.png")));
        assertFalse(policy.isExplicitlyTrusted(url("https://8.8.8.8/img.png")));
        assertFalse(policy.isExplicitlyTrusted(url("http:///img.png")));
        assertTrue(policy(Mode.ALLOW_ALL, List.of()).isExplicitlyTrusted(url("https://8.8.8.8/img.png")));
    }

    @Test
    void readsTheSchemeOfTheBaseUrl() {
        assertEquals("http", policy(Mode.BLOCK_INTERNAL, List.of()).getBaseUrlScheme());
        assertEquals("https", new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of(), "https://polarion.example", 16).getBaseUrlScheme());
        assertEquals("http", new ResourceUrlPolicy(Mode.BLOCK_INTERNAL, List.of(), null, 16).getBaseUrlScheme());
    }

    @Test
    void allowsExplicitlyListedOrigins() {
        // an allowed origin still has to resolve, so the test uses addresses instead of invented names
        ResourceUrlPolicy policy = policy(Mode.BLOCK_INTERNAL, List.of(" 10.0.0.5 ", "127.0.0.1:8443", ""));
        assertTrue(policy.isAllowed(url("http://10.0.0.5/img.png")));
        assertTrue(policy.isAllowed(url("https://127.0.0.1:8443/img.png")));
        assertFalse(policy.isAllowed(url("https://127.0.0.1/img.png")));
        assertFalse(policy.isAllowed(url("http://10.0.0.6/img.png")));
    }

    @Test
    void allowsAnOriginUnderTheSchemeItNames() {
        // what an entry leaves out is not compared, and a scheme names the port it implies
        ResourceUrlPolicy policy = policy(Mode.BLOCK_INTERNAL, List.of("https://10.0.0.5", "http://10.0.0.6:8080"));

        assertTrue(policy.isAllowed(url("https://10.0.0.5/img.png")));
        assertFalse(policy.isAllowed(url("http://10.0.0.5/img.png")));
        assertFalse(policy.isAllowed(url("https://10.0.0.5:8443/img.png")));
        assertTrue(policy.isAllowed(url("http://10.0.0.6:8080/img.png")));
        assertFalse(policy.isAllowed(url("https://10.0.0.6:8080/img.png")));
        assertFalse(policy.isAllowed(url("http://10.0.0.6/img.png")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://10.0.0.5",            // only http and https can be requested
            "https://10.0.0.5:99999",    // no port
            "https://10.0.0.5/secured/", // an origin names no path
            "https://10.0.0.5?q=1",
            "http://",
            "://10.0.0.5"
    })
    void ignoresAnEntryWhichNamesNoOrigin(String entry) {
        ResourceUrlPolicy policy = policy(Mode.ALLOWLIST_ONLY, List.of(entry));

        assertFalse(policy.isAllowed(url("https://10.0.0.5/img.png")));
        assertFalse(policy.isAllowed(url("http://10.0.0.5/img.png")));
    }

    @Test
    void allowlistOnlyRejectsEverythingElse() {
        ResourceUrlPolicy policy = policy(Mode.ALLOWLIST_ONLY, List.of("8.8.4.4"));
        assertTrue(policy.isAllowed(url("https://8.8.4.4/img.png")));
        assertFalse(policy.isAllowed(url("https://8.8.8.8/img.png")));
    }

    @Test
    void tellsWhetherARefusalTurnedOnTheScheme() {
        // an entry naming a scheme allows that spelling of the host and no other
        ResourceUrlPolicy policy = policy(Mode.ALLOWLIST_ONLY, List.of("https://cdn.example.com", "https://other.example.com:8443"));

        assertTrue(policy.isRefusalSchemeSpecific(url("http://cdn.example.com/logo.png")));
        assertFalse(policy.isRefusalSchemeSpecific(url("https://cdn.example.com/logo.png")));
        // a written port keeps the question open, the entry may name the other scheme on that port
        assertTrue(policy.isRefusalSchemeSpecific(url("http://other.example.com:8443/logo.png")));
        assertFalse(policy.isRefusalSchemeSpecific(url("http://unlisted.example.com/logo.png")));
    }

    @Test
    void tellsNothingTurnsOnTheSchemeWhereTheEntryNamesNone() {
        ResourceUrlPolicy policy = policy(Mode.ALLOWLIST_ONLY, List.of("cdn.example.com"));

        assertFalse(policy.isRefusalSchemeSpecific(url("http://cdn.example.com/logo.png")));
        assertFalse(policy.isRefusalSchemeSpecific(url("https://cdn.example.com/logo.png")));
    }

    @Test
    void allowAllRejectsNothingButForeignSchemes() {
        ResourceUrlPolicy policy = policy(Mode.ALLOW_ALL, List.of());
        assertTrue(policy.isAllowed(url("http://127.0.0.1:8080/secret")));
        assertTrue(policy.isAllowed(url("http://169.254.169.254/latest/meta-data/")));
        assertFalse(policy.isAllowed(url("ftp://127.0.0.1/secret")));
    }

    @Test
    @SneakyThrows
    void handlesUnusableBaseUrl() {
        ResourceUrlPolicy policy = new ResourceUrlPolicy(Mode.ALLOWLIST_ONLY, null, null, 16);
        assertFalse(policy.isAllowed(url("http://localhost/polarion/some-resource.png")));
        assertTrue(ResourceUrlPolicy.isPublicAddress(InetAddress.getByName("8.8.8.8")));
    }

    @ParameterizedTest
    @CsvSource({
            "BLOCK_INTERNAL,BLOCK_INTERNAL",
            " ALLOWLIST_ONLY ,ALLOWLIST_ONLY",
            "ALLOW_ALL,ALLOW_ALL",
            // one of the three names, written exactly so, and anything else is the safe mode
            "blockInternal,BLOCK_INTERNAL",
            "block_internal,BLOCK_INTERNAL",
            "allow_all,BLOCK_INTERNAL",
            "ALLOW-ALL,BLOCK_INTERNAL",
            "nonsense,BLOCK_INTERNAL",
            "'',BLOCK_INTERNAL"
    })
    void parsesMode(String value, Mode expected) {
        assertEquals(expected, Mode.parse(value));
    }

    @Test
    void parsesMissingMode() {
        assertEquals(Mode.BLOCK_INTERNAL, Mode.parse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "IMAGE/SVG+XML", "image/jpeg; charset=binary", "font/woff2",
            "application/font-woff", "application/x-font-ttf", "text/css", "application/octet-stream", "  "})
    void allowsResourceContentTypes(String contentType) {
        assertTrue(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowedContentType(contentType));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "text/html", "text/plain", "application/xml"})
    void rejectsForeignContentTypes(String contentType) {
        assertFalse(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowedContentType(contentType));
    }

    @ParameterizedTest
    @CsvSource({
            // the sender named a kind and the content is one: believed
            "image/png,image/png,false",
            "font/woff2,font/woff2,false",
            "image/svg+xml,image/svg+xml,false",
            // a stylesheet and an svg read as text, and the sender named one of those two
            "text/css,text/plain,false",
            "text/css; charset=utf-8,text/plain,false",
            "image/svg+xml,text/plain,false",
            "image/svg+xml,application/xml,false",
            // the sender named a shape the content does not have
            "image/png,text/plain,true",
            "image/png,application/octet-stream,true",
            "font/woff2,application/octet-stream,true",
            "image/png,text/html,true",
            "font/woff2,application/json,true",
            "text/css,text/html,true",
            // the sender named nothing usable, so the content alone answers
            "'',image/png,false",
            "application/octet-stream,image/png,false",
            "application/octet-stream,text/plain,true",
            "'',text/plain,true",
            "'',application/xml,true"
    })
    void judgesTheContentAgainstWhatItsSenderCalledIt(String declared, String sniffed, boolean rejected) {
        assertEquals(rejected, policy(Mode.BLOCK_INTERNAL, List.of()).isRejectedContent(declared, sniffed));
    }

    @Test
    void believesASenderWhichNamedAKindWhenNothingCouldBeDetected() {
        ResourceUrlPolicy policy = policy(Mode.BLOCK_INTERNAL, List.of());

        assertFalse(policy.isRejectedContent("image/png", null));
        assertTrue(policy.isRejectedContent(null, null));
        assertTrue(policy.isRejectedContent("application/octet-stream", null));
    }

    @Test
    void acceptsMissingContentType() {
        assertTrue(policy(Mode.BLOCK_INTERNAL, List.of()).isAllowedContentType(null));
    }

    @Test
    void readsItsConfiguration() {
        PdfExporterExtensionConfiguration configuration = mock(PdfExporterExtensionConfiguration.class);
        when(configuration.getExternalResourcesPolicy()).thenReturn("ALLOWLIST_ONLY");
        when(configuration.getExternalResourcesAllowedOrigins()).thenReturn("8.8.4.4, https://10.0.0.5");
        when(configuration.getExternalResourcesMaxSizeMB()).thenReturn(4);

        try (MockedStatic<PdfExporterExtensionConfiguration> mocked = mockStatic(PdfExporterExtensionConfiguration.class)) {
            mocked.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);

            ResourceUrlPolicy policy = ResourceUrlPolicy.getInstance();
            assertEquals(4L * 1024 * 1024, policy.getMaxResourceBytes());
            assertTrue(policy.isAllowed(url("https://8.8.4.4/img.png")));
            // the entry of that one names https, so the http spelling of it is no allowed origin
            assertTrue(policy.isAllowed(url("https://10.0.0.5/img.png")));
            assertFalse(policy.isAllowed(url("http://10.0.0.5/img.png")));
            assertFalse(policy.isAllowed(url("https://8.8.8.8/img.png")));
        }
    }

    @Test
    void fallsBackWhenTheConfigurationIsEmpty() {
        PdfExporterExtensionConfiguration configuration = mock(PdfExporterExtensionConfiguration.class);

        try (MockedStatic<PdfExporterExtensionConfiguration> mocked = mockStatic(PdfExporterExtensionConfiguration.class)) {
            mocked.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);

            ResourceUrlPolicy policy = ResourceUrlPolicy.getInstance();
            assertEquals(16L * 1024 * 1024, policy.getMaxResourceBytes());
            assertFalse(policy.isAllowed(url("http://127.0.0.1/secret")));
        }
    }

    @Test
    void convertsSizeLimitToBytes() {
        assertEquals(16L * 1024 * 1024, policy(Mode.BLOCK_INTERNAL, List.of()).getMaxResourceBytes());
    }
}
