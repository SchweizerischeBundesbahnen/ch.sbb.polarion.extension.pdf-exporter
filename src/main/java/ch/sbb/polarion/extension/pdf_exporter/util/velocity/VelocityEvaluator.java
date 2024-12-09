package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.util.VelocityContextInitializer;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.ui.server.VelocityFactory;
import com.polarion.core.util.logging.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.Objects;

public class VelocityEvaluator {

    private static final Logger log = Logger.getLogger(VelocityEvaluator.class);

    public @NotNull String evaluateVelocityExpressions(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull String template) {
        return Objects.requireNonNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            VelocityContext velocityContext = createVelocityContext(transaction, documentData);
            StringWriter writer = new StringWriter();
            VelocityEngine velocityEngine = new VelocityFactory().engine();

            try {
                velocityEngine.evaluate(velocityContext, writer, null, template);
                return writer.toString();
            } catch (ParseErrorException | MethodInvocationException | ResourceNotFoundException e) {
                log.error("error during velocity template evaluation", e);
                return template;
            }
        }));
    }

    private @NotNull VelocityContext createVelocityContext(@NotNull ReadOnlyTransaction transaction, @NotNull DocumentData<? extends IUniqueObject> documentData) {
        VelocityContext velocityContext = new VelocityContextInitializer(transaction).create();

        switch (documentData.getDocumentType()) {
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

        velocityContext.put("projectName", documentData.getProjectName());

        return velocityContext;
    }
}
