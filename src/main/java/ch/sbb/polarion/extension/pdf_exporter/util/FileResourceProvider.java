package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FileResourceProvider {

    @Nullable
    String getResourceAsBase64String(@NotNull String resource);

    byte[] getResourceAsBytes(@NotNull String resource);

    /**
     * Tells whether the resource policy forbids requesting this URL. Such a URL must not be left in the
     * HTML either: WeasyPrint would then load it from its own network position.
     */
    default boolean isForbidden(@NotNull String resource) {
        return false;
    }

}
