package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DocumentId {
    @Nullable DocumentProject getDocumentProject();

    @NotNull String getDocumentId();
}
