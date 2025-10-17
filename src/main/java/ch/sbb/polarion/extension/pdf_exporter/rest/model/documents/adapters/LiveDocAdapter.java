package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.generic.service.PolarionBaselineExecutor;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.LiveDocCommentsProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class LiveDocAdapter extends CommonUniqueObjectAdapter {
    public static final String DOC_REVISION_CUSTOM_FIELD = "docRevision";
    public static final String URL_QUERY_PARAM_LANGUAGE = "language";

    private final @NotNull IModule module;

    public LiveDocAdapter(@NotNull IModule module) {
        this.module = module;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IUniqueObject> @NotNull T getUniqueObject() {
        return (T) module;
    }

    @Override
    public @NotNull DocumentId getDocumentId() {
        return new LiveDocId(getDocumentProject(), module.getModuleFolder(), module.getId());
    }

    private @NotNull DocumentProject getDocumentProject() {
        return new DocumentProject(module.getProject());
    }

    @Override
    public @NotNull DocumentType getDocumentType() {
        return DocumentType.LIVE_DOC;
    }

    @Override
    public @NotNull String getTitle() {
        return module.getTitleOrName();
    }

    @Override
    public @NotNull String getRevisionPlaceholder() {
        Object docRevision = module.getCustomField(DOC_REVISION_CUSTOM_FIELD);
        return docRevision != null ? docRevision.toString() : super.getRevisionPlaceholder();
    }

    @Override
    public @NotNull String getContent(@NotNull ExportParams exportParams, @NotNull ReadOnlyTransaction transaction) {
        return PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
            ProxyDocument document = new ProxyDocument(module, (InternalReadOnlyTransaction) transaction);
            Map<String, String> documentParameters = exportParams.getUrlQueryParameters() == null ? Map.of() : exportParams.getUrlQueryParameters();
            DocumentRendererParameters parameters = new DocumentRendererParameters(null, documentParameters.get(URL_QUERY_PARAM_LANGUAGE));
            ModifiedDocumentRenderer documentRenderer = new ModifiedDocumentRenderer((InternalReadOnlyTransaction) transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);

            String internalContent = StringUtils.getEmptyIfNull(Optional.ofNullable(exportParams.getInternalContent()).orElse(document.getHomePageContentHtml()));
            // Process comments in document itself (workitem descriptions aren't rendered yet)
            internalContent = processComments(exportParams, document, internalContent);
            String renderedContent = documentRenderer.render(internalContent);
            // Now process comments again to catch comments in workitem descriptions
            return processComments(exportParams, document, renderedContent);
        });
    }

    private String processComments(@NotNull ExportParams exportParams, @NotNull ProxyDocument document, @NotNull String content) {
        return new LiveDocCommentsProcessor().addLiveDocComments(document, content, exportParams.getRenderComments());
    }

}
