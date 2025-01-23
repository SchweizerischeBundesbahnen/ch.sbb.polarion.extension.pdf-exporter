package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.rp.widget.RichPageScriptRenderer;
import com.polarion.alm.server.api.model.rp.widget.impl.RichPageRenderingContextImpl;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;

public class VelocityEvaluator {

    public @NotNull String evaluateVelocityExpressions(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull String template) {
        return ObjectUtils.requireNotNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            RichPageRenderingContextImpl richPageRenderingContext = new RichPageRenderingContextImpl((InternalReadOnlyTransaction) transaction);
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
}
