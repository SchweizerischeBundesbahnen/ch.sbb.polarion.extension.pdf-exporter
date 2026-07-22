package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test for the Table of Contents (ToC). Renders a document with a {@code <pd4ml:toc>} placeholder and
 * headings spread across pages, then compares the rendered pages against reference images. It guards the native-like
 * ToC styling (dot leaders and WeasyPrint-resolved page numbers) end to end.
 */
class TableOfContentTest extends BasePdfConverterTest {

    private static final String HTML_RESOURCE = "tableOfContent";

    @Override
    protected void setupHeaderFooterSettings() {
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("")
                .headerCenter("Table of Contents Test")
                .headerRight("")
                .footerLeft("")
                .footerCenter("{{ PAGE_NUMBER }}")
                .footerRight("")
                .build());
    }

    @Test
    void testTableOfContentGeneration() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Table of Contents Test")
                .content(readHtmlResource(HTML_RESOURCE))
                .lastRevision("1")
                .revisionPlaceholder("1")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        boolean hasDiff = compareContentUsingReferenceImages(HTML_RESOURCE, converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }
}
