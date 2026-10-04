package ch.sbb.polarion.extension.pdf_exporter.util.html;

import java.util.Map;
import java.util.Optional;

public interface LinkInternalizer {
    Optional<String> inline(Map<String, String> attributes);

    /**
     * Inlines the link of the given document, which an inliner may read for what the link needs of it, as the font
     * families the document names.
     */
    default Optional<String> inlineIn(Map<String, String> attributes, String document) {
        return inline(attributes);
    }
}
