package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.base.BaseWeasyPrintTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicTest extends BaseWeasyPrintTest {

    public static final String SIMPLE = "simple";

    @Test
    void testExportSimpleHtml() {
        assertThat(exportAndGetAsImages(SIMPLE)).size().isEqualTo(1);
    }
}
