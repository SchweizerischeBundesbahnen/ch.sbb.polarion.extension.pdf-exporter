package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveReportId;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.rp.ProxyRichPage;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.rpe.RpeModelAspect;
import com.polarion.alm.shared.rpe.RpeRenderer;
import com.polarion.alm.tracker.model.IRichPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LiveReportAdapter extends CommonUniqueObjectAdapter {
    private final @NotNull IRichPage richPage;

    public LiveReportAdapter(@NotNull IRichPage richPage) {
        this.richPage = richPage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IUniqueObject> @NotNull T getUniqueObject() {
        return (T) richPage;
    }

    @Override
    public @NotNull DocumentId getDocumentId() {
        return new LiveReportId(getDocumentProject(), richPage.getId());
    }

    private @Nullable DocumentProject getDocumentProject() {
        return richPage.getProject() == null ? null : new DocumentProject(richPage.getProject());
    }

    @Override
    public @NotNull DocumentType getDocumentType() {
        return DocumentType.LIVE_REPORT;
    }

    @Override
    public @NotNull String getTitle() {
        return richPage.getTitleOrName();
    }

    @Override
    public @NotNull String getContent(@NotNull ExportParams exportParams, @NotNull ReadOnlyTransaction transaction) {
        return PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
            ProxyRichPage proxyRichPage = new ProxyRichPage(richPage, (InternalReadOnlyTransaction) transaction);

            String html = RpeModelAspect.getPageHtml(proxyRichPage);
            Map<String, String> liveReportParameters = exportParams.getUrlQueryParameters() == null ? Map.of() : exportParams.getUrlQueryParameters();
            StrictMap<String, String> urlParameters = new StrictMapImpl<>(liveReportParameters);
            RpeRenderer richPageRenderer = new RpeRenderer((InternalReadOnlyTransaction) transaction, html, RichTextRenderTarget.PDF_EXPORT, proxyRichPage.getReference(), proxyRichPage.getReference().scope(), urlParameters);
            return richPageRenderer.render(null);
        });
    }

}
