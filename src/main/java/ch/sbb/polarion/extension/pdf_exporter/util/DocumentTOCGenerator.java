package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

public interface DocumentTOCGenerator {
    void addTableOfContent(@NotNull Document document);
}
