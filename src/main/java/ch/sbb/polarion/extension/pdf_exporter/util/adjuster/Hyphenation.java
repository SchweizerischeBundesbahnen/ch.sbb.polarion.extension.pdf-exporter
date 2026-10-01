package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Whether the tables of a document hyphenate their long words of letters.
 *
 * @param language    the language of the document, null where it has none and nothing hyphenates
 * @param turnedOffBy the selectors of the rules of the CSS of the export which turn hyphenation off
 * @param pageBefore  what the template puts in front of the document, the header and the footer, which the rules are
 *                    matched with, as a rule may name an element by its place
 */
public record Hyphenation(@Nullable String language, @NotNull List<String> turnedOffBy, @NotNull String pageBefore) {

    public static final Hyphenation NONE = new Hyphenation(null, List.of());

    public Hyphenation(@Nullable String language, @NotNull List<String> turnedOffBy) {
        this(language, turnedOffBy, "");
    }
}
