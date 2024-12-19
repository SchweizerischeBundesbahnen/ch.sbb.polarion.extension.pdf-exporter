package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id;

import org.jetbrains.annotations.NotNull;

public class LiveDocId implements DocumentId {
    private final @NotNull DocumentProject documentProject;
    private final @NotNull String spaceId;
    private final @NotNull String documentName;

    public LiveDocId(@NotNull DocumentProject documentProject, @NotNull String spaceId, @NotNull String documentName) {
        this.documentProject = documentProject;
        this.spaceId = spaceId;
        this.documentName = documentName;
    }

    @Override
    public @NotNull DocumentProject getDocumentProject() {
        return documentProject;
    }

    public @NotNull String getSpaceId() {
        return spaceId;
    }

    @Override
    public @NotNull String getDocumentId() {
        return documentName;
    }

    public static LiveDocId from(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName) {
        return new LiveDocId(new DocumentProject(projectId, projectId), spaceId, documentName);
    }
}
