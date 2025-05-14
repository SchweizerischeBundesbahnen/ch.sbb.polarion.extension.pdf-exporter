package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.ServerUiContext;
import com.polarion.alm.server.api.model.rp.widget.RichPageScriptRenderer;
import com.polarion.alm.server.api.model.rp.widget.impl.RichPageRenderingContextImpl;
import com.polarion.alm.server.html.HtmlFragmentParser;
import com.polarion.alm.server.html.ServerHtmlNodeFactory;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.impl.ScopeImpl;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.HtmlRichPageParameters;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictMap;
import com.polarion.alm.shared.html.HtmlElement;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.core.util.StringUtils;
import com.polarion.portal.internal.shared.navigation.ProjectScope;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class VelocityEvaluator {

    public @NotNull String evaluateVelocityExpressions(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull String template) {
        return ObjectUtils.requireNotNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            RichPageRenderingContextImpl richPageRenderingContext = new RichPageRenderingContextImpl((InternalReadOnlyTransaction) transaction);
            richPageRenderingContext.setPageParameters(getPageParameters(documentData));
            RichPageScriptRenderer richPageScriptRenderer = new RichPageScriptRenderer(richPageRenderingContext, template, documentData.getId().getDocumentId());
            VelocityContext velocityContext = richPageScriptRenderer.velocityContext();
            updateVelocityContext(velocityContext, documentData);
            return richPageScriptRenderer.renderHtml();
        }));
    }

    private void updateVelocityContext(@NotNull VelocityContext velocityContext, @NotNull DocumentData<? extends IUniqueObject> documentData) {

        switch (documentData.getType()) {
            case LIVE_DOC -> {
                if (documentData.getDocumentObject() instanceof IModule) {
                    velocityContext.put("document", documentData.getDocumentObject());
                }
            }
            case LIVE_REPORT -> {
                if (documentData.getDocumentObject() instanceof IRichPage) {
                    velocityContext.put("page", documentData.getDocumentObject());
                }
            }
            case TEST_RUN -> {
                if (documentData.getDocumentObject() instanceof ITestRun) {
                    velocityContext.put("testrun", documentData.getDocumentObject());
                }
            }
            case WIKI_PAGE -> {
                if (documentData.getDocumentObject() instanceof IWikiPage) {
                    velocityContext.put("page", documentData.getDocumentObject());
                }
            }
            case BASELINE_COLLECTION -> {
                if (documentData.getDocumentObject() instanceof IBaselineCollection) {
                    velocityContext.put("collection", documentData.getDocumentObject());
                }
            }
        }

        velocityContext.put("projectName", documentData.getId().getDocumentProject() != null ? documentData.getId().getDocumentProject().getName() : "");
    }

    /**
     * Extracts page parameters from the document content.
     * The implementation idea is taken from {@link com.polarion.alm.shared.rpe.RpeRenderer}.
     */
    @VisibleForTesting
    ImmutableStrictMap<String, RichPageParameter> getPageParameters(@NotNull DocumentData<? extends IUniqueObject> documentData) {
        String content = documentData.getContent();
        if (StringUtils.isEmpty(content)) {
            return new ImmutableStrictMap<>();
        }
        HtmlFragmentParser fragmentParser = new HtmlFragmentParser(content);
        HtmlElement htmlElement = HtmlRichPageParameters.findElement(fragmentParser.getHtmlNodes(new ServerHtmlNodeFactory()));
        if (htmlElement == null) {
            return new ImmutableStrictMap<>();
        }
        Scope scope = new ScopeImpl(new ProjectScope(documentData.getDocumentObject().getProjectId()));
        return new HtmlRichPageParameters(htmlElement, ServerUiContext.getInstance(), scope).get(null).toImmutable();
    }
}
