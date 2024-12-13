package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id;

import org.jetbrains.annotations.NotNull;

public class TestRuntId implements DocumentId {
    private final @NotNull DocumentProject documentProject;
    private final @NotNull String testRunId;

    public TestRuntId(@NotNull DocumentProject documentProject, @NotNull String testRunId) {
        this.documentProject = documentProject;
        this.testRunId = testRunId;
    }

    @Override
    public @NotNull DocumentProject getDocumentProject() {
        return documentProject;
    }

    @Override
    public @NotNull String getDocumentId() {
        return testRunId;
    }
}
