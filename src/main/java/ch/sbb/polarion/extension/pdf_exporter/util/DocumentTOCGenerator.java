package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;

public interface DocumentTOCGenerator {
    @NotNull String addTableOfContent(@NotNull String html);
}
