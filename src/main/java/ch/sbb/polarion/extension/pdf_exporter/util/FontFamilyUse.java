package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Tells whether a font family is named anywhere in the given texts, outside the {@code @font-face} rules which declare
 * families and outside comments: in a rule of a stylesheet, a style attribute or a custom property. A family no text names is never drawn,
 * so its {@code @font-face} rule only carries a font into the export which nothing uses.
 * <p>
 * It errs on the side of keeping a font: a name is found wherever it stands as a word of its own, in a value or not.
 * </p>
 */
public final class FontFamilyUse implements Predicate<String> {

    /**
     * What says nothing of the use of a family: a rule which declares one, a comment, as the licence of a stylesheet
     * which names its maker, and the payload of a data url, as the images and fonts already embedded, by far the most
     * of a document, which no search has to read through. A data url carries no brace and no asterisk.
     */
    private static final Pattern NOT_A_USE = Pattern.compile("@font-face\\s*\\{[^}]*}|/\\*.*?\\*/|;base64,[A-Z0-9+/=]+",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final List<String> texts;

    private FontFamilyUse(@NotNull List<String> texts) {
        this.texts = texts;
    }

    /** The use of families in the given texts, as a document and the stylesheets it embeds. */
    public static @NotNull FontFamilyUse in(@NotNull String... texts) {
        return new FontFamilyUse(Arrays.stream(texts).map(text -> NOT_A_USE.matcher(text).replaceAll("")).toList());
    }

    /**
     * Whether the family is named as a word of its own, its words apart by any space, in any case and in quotes or not.
     * A name standing in a path or a word, as {@code fontawesome} in {@code /fontawesome-6.2.0/}, does not count.
     */
    @Override
    public boolean test(@NotNull String family) {
        String[] words = family.trim().split("\\s+");
        String name = String.join("\\s+", Arrays.stream(words).map(Pattern::quote).toList());
        Pattern named = Pattern.compile("(?<![\\w\\-./@])" + name + "(?![\\w\\-.])", Pattern.CASE_INSENSITIVE);
        return texts.stream().anyMatch(text -> named.matcher(text).find());
    }
}
