package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that documents with invalid CSS (e.g. '%' in unexpected context) are still exported to PDF successfully,
 * and that CSS parse warnings are logged.
 */
public class InvalidCssTest extends BaseWeasyPrintTest {

    public static final String INVALID_CSS = "invalidCss";

    @Test
    void testExportHtmlWithInvalidCssProducesPdfAndLogsWarnings() {
        Logger cssUtilsLogger = Logger.getLogger(CssUtils.class.getName());
        List<LogRecord> logRecords = new ArrayList<>();
        Handler testHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        cssUtilsLogger.addHandler(testHandler);
        try {
            assertThat(exportAndGetAsImages(INVALID_CSS)).size().isEqualTo(1);

            assertThat(logRecords)
                    .isNotEmpty()
                    .allMatch(r -> r.getLevel() == Level.WARNING)
                    .anyMatch(r -> r.getMessage().contains("Failed to parse CSS"));
        } finally {
            cssUtilsLogger.removeHandler(testHandler);
        }
    }
}
