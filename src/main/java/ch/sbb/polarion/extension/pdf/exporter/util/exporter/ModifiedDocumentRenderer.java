package ch.sbb.polarion.extension.pdf.exporter.util.exporter;

import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictList;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.api.utils.html.impl.HtmlBuilder;
import com.polarion.alm.shared.api.utils.internal.Optimizations;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.shared.dle.lazyload.DleLazyLoadParams;
import com.polarion.alm.shared.html.HtmlNode;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import com.polarion.alm.shared.rt.document.RenderedDlePart;
import com.polarion.alm.shared.rt.document.ServerRichTextDocumentBase;
import com.polarion.alm.shared.rt.parts.FieldRichTextRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Copy of {@link com.polarion.alm.shared.dle.document.DocumentRenderer}.
 * Used to implement 'page break' feature using polarion native markers.
 */
public class ModifiedDocumentRenderer {
    @NotNull
    private final InternalReadOnlyTransaction transaction;
    @NotNull
    private final InternalDocument document;
    @NotNull
    private final RichTextRenderTarget renderTarget;
    @NotNull
    private final RichTextRenderingContext renderingContext;
    private ServerRichTextDocumentBase richTextDocument;
    private boolean fixLevel;

    public ModifiedDocumentRenderer(@NotNull InternalReadOnlyTransaction transaction, @NotNull InternalDocument document, @NotNull RichTextRenderTarget renderTarget, @NotNull DocumentRendererParameters parameters) {
        this(transaction, document, renderTarget, parameters, (DleLazyLoadParams)null, (StrictMap)null, false);
    }

    private ModifiedDocumentRenderer(@NotNull InternalReadOnlyTransaction transaction, @NotNull InternalDocument document, @NotNull RichTextRenderTarget renderTarget, @NotNull DocumentRendererParameters parameters,
                                     @Nullable DleLazyLoadParams lazyLoadParams, @Nullable StrictMap<String, Boolean> resolvedComments, boolean oldVersionOfDocument) {
        this.fixLevel = true;
        this.transaction = transaction;
        this.document = document;
        this.renderTarget = renderTarget;
        this.renderingContext = new RichTextRenderingContext(transaction.context(), renderTarget);
        if (lazyLoadParams != null) {
            this.renderingContext.setDocumentLazyLoad(lazyLoadParams.isDocumentLazyLoad());
            this.renderingContext.setCurrentDocumentRevision(lazyLoadParams.getCurrentDocumentRevision());
        }

        FieldRichTextRenderer renderer = null;
        if (this.renderingContext.documentLazyLoad()) {
            renderer = transaction.documents().createPlaceholderFieldRichTextRenderer();
        } else {
            renderer = transaction.documents().createFieldRichTextRenderer();
        }

        if (this.renderingContext.documentLazyLoad()) {
            this.renderingContext.setDocumentOutlineNumber(document.fields().usesOutlineNumbering().getIfCan());
        }

        if (renderTarget.equals(RichTextRenderTarget.PREVIEW)) {
            this.renderingContext.setMainObjectReference(document.getReferenceToCurrent());
        } else {
            this.renderingContext.setMainObjectReference(document.getReference());
            this.renderingContext.setDocumentRender(true);
        }

        this.renderingContext.dle().setResolvedComments(resolvedComments);
        this.renderingContext.dle().setOldVersionOfDocument(oldVersionOfDocument);
        this.renderingContext.dle().setQuery(parameters.query);
        this.renderingContext.dle().setLanguage(parameters.language);
        this.renderingContext.setExportId(parameters.exportId);
        this.renderingContext.setRenderWikiAsSources(parameters.renderWikiAsSources);
        this.renderingContext.setSkipResolvableContent(parameters.skipResolvableContent);
        this.renderingContext.dle().setResolvedCommentMarksExcluded(parameters.resolvedCommentMarksExcluded);
        this.renderingContext.setTransaction(transaction);
        this.renderingContext.dle().setFieldRenderer(renderer);
        renderer.initialize();
    }

    @NotNull
    public String render(@NotNull String html) {
        return ObjectUtils.requireNotNull(this.transaction.utils().executeWithOptimizations(Optimizations.ALL, () -> this.renderImpl(html)));
    }

    @NotNull
    @SuppressWarnings("java:S1874") //this logic is reused from the internal API
    private String renderImpl(@NotNull String html) {
        this.createDocumentAndPreProcess(html);
        HtmlBuilder builder = this.renderTarget.selectBuilderTarget(this.transaction.context().createHtmlBuilderFor());
        this.richTextDocument.render(builder, this.renderingContext);
        return builder.toString();
    }

    @NotNull
    public StrictList<RenderedDlePart> renderAllParts(@NotNull String html) {
        return ObjectUtils.requireNotNull(this.transaction.utils().executeWithOptimizations(Optimizations.ALL, () -> {
            this.createDocumentAndPreProcess(html);
            return this.richTextDocument.renderAllParts(this.renderingContext);
        }));
    }

    private void createDocumentAndPreProcess(@NotNull String html) {
        StrictList<HtmlNode> htmlNodes = this.transaction.documents().parseHtmlFragment(html);
        this.richTextDocument = new ModifiedServerRichTextDocumentFullyLoaded(this.document, htmlNodes, this.renderingContext.dle().getQuery(), this.fixLevel);
        this.preProcess(this.richTextDocument);
    }

    protected void preProcess(@NotNull ServerRichTextDocumentBase richTextDocument) {
        //nothing here
    }

    @NotNull
    public RichTextRenderingContext getRenderingContext() {
        return this.renderingContext;
    }

    @NotNull
    public ServerRichTextDocumentBase getRichTextDocument() {
        return this.richTextDocument;
    }
}
