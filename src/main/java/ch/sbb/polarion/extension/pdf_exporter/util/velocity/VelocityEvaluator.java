package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.generic.util.ObjectUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.util.VelocityContextInitializer;
import com.polarion.alm.ui.server.VelocityFactory;
import com.polarion.core.util.logging.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;

public class VelocityEvaluator {

    private static final Logger log = Logger.getLogger(VelocityEvaluator.class);

    public @NotNull String evaluateVelocityExpressions(@NotNull DocumentData documentData, @NotNull String template) {
        return ObjectUtils.requireNotNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
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

    private @NotNull VelocityContext createVelocityContext(@NotNull ReadOnlyTransaction transaction, @NotNull DocumentData documentData) {
        VelocityContext velocityContext = new VelocityContextInitializer(transaction).create();
        if (documentData.getDocument() != null) {
            velocityContext.put("document", documentData.getDocument());
        }
        if (documentData.getRichPage() != null) {
            velocityContext.put("page", documentData.getRichPage());
        }
        if (documentData.getTestRun() != null) {
            velocityContext.put("testrun", documentData.getTestRun());
        }
        if (documentData.getWikiPage() != null) {
            velocityContext.put("page", documentData.getWikiPage());
        }
        velocityContext.put("projectName", documentData.getProjectName());
        return velocityContext;
    }
}
