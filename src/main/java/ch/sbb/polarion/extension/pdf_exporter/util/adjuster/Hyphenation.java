package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Whether the tables of a document hyphenate their long words of letters.
 *
 * @param language    the language of the document, null where it has none and nothing hyphenates
 * @param turnedOffBy the selectors of the rules of the CSS of the export which turn hyphenation off
 */
public record Hyphenation(@Nullable String language, @NotNull List<String> turnedOffBy) {

    public static final Hyphenation NONE = new Hyphenation(null, List.of());
}
