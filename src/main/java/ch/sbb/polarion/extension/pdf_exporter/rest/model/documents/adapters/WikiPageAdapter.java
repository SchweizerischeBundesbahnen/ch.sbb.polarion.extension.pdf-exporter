package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveReportId;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.WikiRenderer;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.tracker.model.IWikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WikiPageAdapter extends CommonUniqueObjectAdapter {
    private final @NotNull IWikiPage wikiPage;

    public WikiPageAdapter(@NotNull IWikiPage wikiPage) {
        this.wikiPage = wikiPage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IUniqueObject> @NotNull T getUniqueObject() {
        return (T) wikiPage;
    }

    @Override
    public @NotNull DocumentId getDocumentId() {
        return new LiveReportId(getDocumentProject(), wikiPage.getId());
    }

    private @Nullable DocumentProject getDocumentProject() {
        return wikiPage.getProject() == null ? null : new DocumentProject(wikiPage.getProject());
    }

    @Override
    public @NotNull DocumentType getDocumentType() {
        return DocumentType.WIKI_PAGE;
    }

    @Override
    public @NotNull String getTitle() {
        return wikiPage.getTitleOrName();
    }

    @Override
    public @NotNull String getContent(@NotNull ExportParams exportParams, @NotNull ReadOnlyTransaction transaction) {
        return PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
            @NotNull String projectId = wikiPage.getProject() != null ? wikiPage.getProject().getId() : "";
            @NotNull String locationPath = Objects.requireNonNull(exportParams.getLocationPath());
            @Nullable String revision = exportParams.getRevision();
            return new WikiRenderer().render(projectId, locationPath, revision);
        });
    }

}
