package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.exporter.WeasyPrintExporter;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for {@link BaseWeasyPrintTest#IMPL_NAME_PARAM} parameter existence.
 */
public class ParamSetCondition implements ExecutionCondition {

    private static final Logger logger = LoggerFactory.getLogger(ParamSetCondition.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        String implValue = System.getProperty(BaseWeasyPrintTest.IMPL_NAME_PARAM);
        if (implValue == null) {
            logger.info("Param {} doesn't set, skipping weasyprint test", BaseWeasyPrintTest.IMPL_NAME_PARAM);
            return ConditionEvaluationResult.disabled("required param doesn't exist");
        }
        WeasyPrintExporter exporter = WeasyPrintExporter.IMPL_REGISTRY.get(implValue.toLowerCase());
        if (exporter == null) {
            logger.info("Param {} contains unsupported value '{}', skipping weasyprint test", BaseWeasyPrintTest.IMPL_NAME_PARAM, implValue);
            return ConditionEvaluationResult.disabled("unsupported param");
        } else {
            return ConditionEvaluationResult.enabled("ok");
        }
    }
}
