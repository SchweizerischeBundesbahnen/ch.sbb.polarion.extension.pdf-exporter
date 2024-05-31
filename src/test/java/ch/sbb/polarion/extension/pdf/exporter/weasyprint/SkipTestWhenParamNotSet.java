package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Skips test execution (marks as 'SKIPPED') if there is no parameter specified. Parameter check logic implemented in {@link ParamSetCondition}.
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ParamSetCondition.class)
public @interface SkipTestWhenParamNotSet {
}
