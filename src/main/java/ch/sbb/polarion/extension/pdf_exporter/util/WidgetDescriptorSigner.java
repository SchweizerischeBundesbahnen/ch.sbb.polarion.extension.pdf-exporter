package ch.sbb.polarion.extension.pdf_exporter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Signs the widget descriptors that travel through the browser and verifies them when they come back.
 * <p>
 * A widget descriptor carries the query its REST endpoint executes, so an unsigned one would let any reader of a
 * report page run an arbitrary Lucene query, or arbitrary SQL, through that endpoint. The key is 256 random bits
 * generated once per server start and never leaves the JVM. A restart therefore invalidates the descriptors of
 * pages that are already open; those pages report the failure and are fixed by a reload, which is preferable to
 * persisting a secret.
 */
public final class WidgetDescriptorSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SecretKeySpec key;

    private WidgetDescriptorSigner() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        key = new SecretKeySpec(secret, ALGORITHM);
    }

    public static WidgetDescriptorSigner getInstance() {
        return SignerHolder.INSTANCE;
    }

    /**
     * Serializes a descriptor into the opaque, base64url encoded form the browser passes around.
     */
    public @NotNull String encode(@NotNull Object descriptor) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(OBJECT_MAPPER.writeValueAsBytes(descriptor));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not encode the widget descriptor", e);
        }
    }

    /**
     * Reads back what {@link #encode(Object)} produced. Callers must verify the signature first.
     */
    public @NotNull <T> T decode(@NotNull String encodedDescriptor, @NotNull Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(Base64.getUrlDecoder().decode(encodedDescriptor), type);
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalArgumentException("Could not decode the widget descriptor", e);
        }
    }

    public @NotNull String sign(@NotNull String encodedDescriptor) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(encodedDescriptor.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the widget descriptor", e);
        }
    }

    /**
     * Compares in constant time: a timing-sensitive comparison would leak the expected signature byte by byte.
     */
    public boolean verify(@Nullable String encodedDescriptor, @Nullable String signature) {
        if (encodedDescriptor == null || signature == null) {
            return false;
        }
        byte[] expected = sign(encodedDescriptor).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    private static class SignerHolder {
        private static final WidgetDescriptorSigner INSTANCE = new WidgetDescriptorSigner();
    }
}
