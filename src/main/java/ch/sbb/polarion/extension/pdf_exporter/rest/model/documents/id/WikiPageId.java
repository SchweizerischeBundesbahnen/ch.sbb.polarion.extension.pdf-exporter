package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WikiPageId implements DocumentId {
    private final @Nullable DocumentProject documentProject;
    private final @NotNull String pageId;

    public WikiPageId(@Nullable DocumentProject documentProject, @NotNull String pageId) {
        this.documentProject = documentProject;
        this.pageId = pageId;
    }

    @Override
    public @Nullable DocumentProject getDocumentProject() {
        return documentProject;
    }

    @Override
    public @NotNull String getDocumentId() {
        return pageId;
    }
}
