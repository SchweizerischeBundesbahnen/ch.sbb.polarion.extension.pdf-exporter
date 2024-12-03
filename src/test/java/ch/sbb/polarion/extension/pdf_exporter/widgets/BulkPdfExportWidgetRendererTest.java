package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BulkPdfExportWidgetRendererTest {

    @Test
    void testGetItemsType() {
        BulkPdfExportWidgetRenderer bulkPdfExportWidgetRenderer = mock(BulkPdfExportWidgetRenderer.class);
        when(bulkPdfExportWidgetRenderer.getItemsType(any())).thenCallRealMethod();

        assertEquals(DocumentType.LIVE_DOC, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.Document));
        assertEquals(DocumentType.LIVE_REPORT, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.RichPage));
        assertEquals(DocumentType.TEST_RUN, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.TestRun));
        assertThrows(IllegalArgumentException.class, () -> bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.WorkItem));
    }

    @Test
    void testGetWidgetItemsType() {
        BulkPdfExportWidgetRenderer bulkPdfExportWidgetRenderer = mock(BulkPdfExportWidgetRenderer.class);
        when(bulkPdfExportWidgetRenderer.getWidgetItemsType(any())).thenCallRealMethod();

        assertEquals("Documents", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.Document));
        assertEquals("Pages", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.RichPage));
        assertEquals("Test Runs", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.TestRun));
        assertThrows(IllegalArgumentException.class, () -> bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.WorkItem));
    }
}
