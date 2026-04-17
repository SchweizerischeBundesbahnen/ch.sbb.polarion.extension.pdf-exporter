package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.generic.service.PolarionBaselineExecutor;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.model.LiveDocComment;
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
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LiveDocAdapter extends CommonUniqueObjectAdapter {
    public static final String DOC_REVISION_CUSTOM_FIELD = "docRevision";

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
            ModifiedDocumentRenderer documentRenderer = getDocumentRenderer(exportParams, (InternalReadOnlyTransaction) transaction, document);

            String internalContent = StringUtils.getEmptyIfNull(Optional.ofNullable(exportParams.getInternalContent()).orElseGet(document::getHomePageContentHtml));
            Set<String> renderedCommentIds = new LinkedHashSet<>();
            LiveDocCommentsProcessor commentsProcessor = new LiveDocCommentsProcessor();
            Map<String, LiveDocComment> liveDocComments = commentsProcessor.getLiveDocComments(document, exportParams.getRenderComments());
            boolean renderNative = exportParams.isRenderNativeComments();
            // Process comments in document itself (workitem descriptions aren't rendered yet)
            internalContent = commentsProcessor.addLiveDocComments(internalContent, liveDocComments, renderNative, renderedCommentIds);
            String renderedContent = documentRenderer.render(internalContent);
            // Now process comments again to catch comments in workitem descriptions
            renderedContent = commentsProcessor.addLiveDocComments(renderedContent, liveDocComments, renderNative, renderedCommentIds);
            if (exportParams.isIncludeUnreferencedComments()) {
                // And the last: render unreferenced comments if needed
                renderedContent = commentsProcessor.addUnreferencedComments(renderedContent, liveDocComments, renderNative, renderedCommentIds);
            }
            return renderedContent;
        });
    }

    @VisibleForTesting
    static @NonNull ModifiedDocumentRenderer getDocumentRenderer(@NonNull ExportParams exportParams, InternalReadOnlyTransaction transaction, ProxyDocument document) {
        Map<String, String> documentParameters = exportParams.getUrlQueryParameters() == null ? Map.of() : exportParams.getUrlQueryParameters();
        DocumentRendererParameters parameters = new DocumentRendererParameters(documentParameters.get(ExportParams.URL_QUERY_PARAM_QUERY), documentParameters.get(ExportParams.URL_QUERY_PARAM_LANGUAGE));
        return new ModifiedDocumentRenderer(transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
    }

}
