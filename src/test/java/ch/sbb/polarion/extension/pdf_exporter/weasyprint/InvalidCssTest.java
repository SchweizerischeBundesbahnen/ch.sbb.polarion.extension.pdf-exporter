package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies that documents with invalid CSS (e.g. '%' in unexpected context) are still exported to PDF successfully,
 * and that CSS parse warnings are logged.
 */
class InvalidCssTest extends BasePdfConverterTest {

    @Test
    void testExportWithInvalidCssProducesPdfAndLogsWarnings() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        String contentWithInvalidCss = """
                <table style="page-break-inside:avoid; width:50%%invalid">
                    <tr><td><p>Work item with invalid CSS</p></td></tr>
                </table>
                <p>Normal paragraph</p>""";

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .content(contentWithInvalidCss)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        Logger mockLogger = mock(Logger.class);
        CssUtils.setLogger(mockLogger);
        try {
            byte[] pdfBytes = converter.convertToPdf(params, null);

            assertThat(pdfBytes).isNotNull().isNotEmpty();

            verify(mockLogger, atLeastOnce()).warn(argThat((String msg) -> msg.contains("Failed to parse CSS")));
        } finally {
            CssUtils.setLogger(Logger.getLogger(CssUtils.class));
        }
    }
}
