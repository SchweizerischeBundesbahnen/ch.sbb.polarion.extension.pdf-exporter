package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BasicTest extends BasePdfConverterTest {

    public static final String SIMPLE = "simple";

    @Test
    void testExportSimpleHtml() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        byte[] pdf = exportLiveDoc("A simple document", readHtmlResource(SIMPLE), params);

        assertThat(getAllPagesAsImagesAndLogAsReports(SIMPLE, pdf)).size().isEqualTo(1);
    }
}
