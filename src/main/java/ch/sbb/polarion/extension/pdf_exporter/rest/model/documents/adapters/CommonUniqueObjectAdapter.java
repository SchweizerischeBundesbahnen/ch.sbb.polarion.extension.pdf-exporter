package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentBaseline;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class CommonUniqueObjectAdapter implements IUniqueObjectAdapter {

    @Override
    public @Nullable String getRevision() {
        return getUniqueObject().getRevision();
    }

    @Override
    public @NotNull String getLastRevision() {
        return getUniqueObject().getLastRevision();
    }

    @Override
    public @NotNull String getRevisionPlaceholder() {
        return getRevision() != null ? getRevision() : getLastRevision();
    }

    @Override
    public @NotNull String getContent(@NotNull ExportParams exportParams) {
        return Objects.requireNonNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> getContent(exportParams, transaction)));
    }

    @Override
    public @NotNull DocumentBaseline getDocumentBaseline() {
        return Objects.requireNonNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(this::getDocumentBaseline));
    }

    @Override
    public @NotNull DocumentBaseline getDocumentBaseline(@NotNull ReadOnlyTransaction transaction) {
        IProject project = getUniqueObject().getProject();
        if (project != null) {
            String revision = getRevision() != null ? getRevision() : getLastRevision();

            ITrackerService trackerService = ((InternalReadOnlyTransaction) transaction).services().trackerService();
            IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) trackerService.getTrackerProject(project).getBaselinesManager();
            IBaseline projectBaseline = baselinesManager.getRevisionBaseline(revision);
            IBaseline moduleBaseline = baselinesManager.getRevisionBaseline(getUniqueObject(), revision);

            return DocumentBaseline.from(projectBaseline, moduleBaseline);
        } else {
            return DocumentBaseline.empty();
        }
    }

}
