package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class WildcardUtils {

    public static @NotNull String toRegex(@Nullable String wildcard) {
        if (wildcard == null || wildcard.isEmpty()) {
            return ".*";
        }

        String regex = wildcard
                .replace(".", "\\.")
                .replace("?", ".")
                .replace("*", ".*");

        return "^" + regex + "$";
    }

    public static boolean matches(@Nullable String text, @Nullable String wildcard) {
        if (text == null) {
            return false;
        }
        String regex = toRegex(wildcard);
        return RegexMatcher.get(regex, RegexMatcher.CASE_INSENSITIVE).anyMatch(text);
    }
}
