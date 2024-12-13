package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.IUniqueObjectAdapter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.LiveDocAdapter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.LiveReportAdapter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.TestRunAdapter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters.WikiPageAdapter;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UniqueObjectConverter {

    @Getter
    private final @NotNull IUniqueObjectAdapter moduleAdapter;
    private boolean withContent;
    private @Nullable ExportParams exportParams;

    public UniqueObjectConverter(@NotNull IUniqueObject uniqueObject) {
        if (uniqueObject instanceof IModule module) {
            moduleAdapter = new LiveDocAdapter(module);
        } else if (uniqueObject instanceof IRichPage richPage) {
            moduleAdapter = new LiveReportAdapter(richPage);
        } else if (uniqueObject instanceof IWikiPage wikiPage) {
            moduleAdapter = new WikiPageAdapter(wikiPage);
        } else if (uniqueObject instanceof ITestRun testRun) {
            moduleAdapter = new TestRunAdapter(testRun);
        } else {
            throw new IllegalArgumentException("Unsupported unique object type: " + uniqueObject.getClass());
        }
    }

    public UniqueObjectConverter(@NotNull ModelObject modelObject) {
        this((IUniqueObject) modelObject.getOldApi());
    }

    public UniqueObjectConverter withExportParams(ExportParams exportParams) {
        this.exportParams = exportParams;
        return this;
    }

    public UniqueObjectConverter withContent(boolean withContent) {
        this.withContent = withContent;
        return this;
    }

    public @NotNull <T extends IUniqueObject> DocumentData<T> toDocumentData() {
        return Objects.requireNonNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(this::toDocumentData));
    }

    public @NotNull <T extends IUniqueObject> DocumentData<T> toDocumentData(@NotNull ReadOnlyTransaction transaction) {
        String baselineRevision = getBaselineRevision();
        return PolarionBaselineExecutor.executeInBaseline(baselineRevision, transaction, () -> DocumentData.<T>builder()
                .documentObject(moduleAdapter.getUniqueObject())
                .id(moduleAdapter.getDocumentId())
                .type(moduleAdapter.getDocumentType())
                .title(moduleAdapter.getTitle())
                .revision(moduleAdapter.getRevision())
                .lastRevision(moduleAdapter.getLastRevision())
                .revisionPlaceholder(moduleAdapter.getRevisionPlaceholder())
                .baseline(moduleAdapter.getDocumentBaseline(transaction))
                .content(withContent && exportParams != null ? moduleAdapter.getContent(exportParams, transaction) : null)
                .build());
    }

    private @Nullable String getBaselineRevision() {
        return exportParams != null ? exportParams.getBaselineRevision() : null;
    }

}
