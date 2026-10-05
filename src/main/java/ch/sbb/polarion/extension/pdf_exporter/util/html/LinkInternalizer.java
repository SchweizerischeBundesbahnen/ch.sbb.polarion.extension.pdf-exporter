package ch.sbb.polarion.extension.pdf_exporter.util.html;

import java.util.Map;
import java.util.Optional;

public interface LinkInternalizer {
    Optional<String> inline(Map<String, String> attributes);

    /**
     * Inlines the link of a document, which an inliner may read for what the link needs of it, as the font families the
     * document names.
     *
     * @param styles the text of the document which can name a font family: its style elements and style attributes
     */
    default Optional<String> inlineIn(Map<String, String> attributes, String styles) {
        return inline(attributes);
    }
}
