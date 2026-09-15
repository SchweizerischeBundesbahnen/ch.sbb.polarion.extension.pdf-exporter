package ch.sbb.polarion.extension.pdf_exporter.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hashes the built-in values of a setting, and recognizes the ones of the versions shipped before settings remembered
 * what they copied.
 * <p>
 * A custom value copied from the built-in values stores the hash of what it copied, so a copy nobody edited and a newer
 * version of the built-in values are both recognized by that hash. A setting stored before carries no hash: the resource
 * {@value #LEGACY_RESOURCE} lists the hashes of the built-in values of every version shipped until then. It is complete,
 * and a later change of the built-in values does not go into it.
 * </p>
 */
final class BuiltInValues {

    static final String LEGACY_RESOURCE = "default/legacy-built-in-values.json";

    private BuiltInValues() {
    }

    /**
     * @return the hash of a version: each field with unix line endings and without the whitespace around it, as
     * storing a setting trims its values, the fields joined by a NUL character
     */
    static @NotNull String hash(@Nullable String... fields) {
        String joined = Arrays.stream(fields).map(BuiltInValues::normalized).collect(Collectors.joining("\0"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * @return whether a hash is the one of built-in values a version shipped before settings remembered what they copied
     */
    static boolean isLegacy(@NotNull String feature, @Nullable String hash) {
        return hash != null && Legacy.HASHES.getOrDefault(feature, Set.of()).contains(hash);
    }

    private static @NotNull String normalized(@Nullable String value) {
        return value == null ? "" : value.replace("\r\n", "\n").strip();
    }

    /**
     * Read once, on first use: the resource is part of the extension and does not change while it runs.
     */
    private static final class Legacy {
        private static final Map<String, Set<String>> HASHES = load();

        private static Map<String, Set<String>> load() {
            try (InputStream stream = Objects.requireNonNull(BuiltInValues.class.getClassLoader().getResourceAsStream(LEGACY_RESOURCE),
                    "Resource " + LEGACY_RESOURCE + " not found")) {
                Map<String, List<String>> hashes = new ObjectMapper().readValue(stream, new TypeReference<>() {
                });
                return hashes.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + LEGACY_RESOURCE, e);
            }
        }
    }
}
