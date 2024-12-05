package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetAccessor;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BulkPdfExportWidgetRendererTest {
    @Test
    void testConstructor() {
        // Arrange
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class);
        FieldsParameter columnsParameter = new FieldsParameterImpl.Builder("TEST").fields(List.of("elements")).build();
        SortingParameter sortingParameter = mock(SortingParameter.class);
        DataSetAccessor dataSetAccessor = mock(DataSetAccessor.class);
        DataSet dataSet = mock(DataSet.class);
        CompositeParameter advanced = mock(CompositeParameter.class);
        IntegerParameter top = mock(IntegerParameter.class);

        when(context.parameter(anyString())).thenReturn(dataSetParameter);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.BaselineCollection);
        when(dataSetParameter.get("columns")).thenReturn(columnsParameter);
        when(dataSetParameter.get("sortBy")).thenReturn(sortingParameter);

        when(dataSetParameter.getFor()).thenReturn(dataSetAccessor);
        when(dataSetParameter.getFor().sort(null)).thenReturn(dataSetAccessor);
        when(dataSetAccessor.revision(null)).thenReturn(dataSet);

        when(context.parameter("advanced")).thenReturn(advanced);
        when(advanced.get("top")).thenReturn(top);
        when(top.value()).thenReturn(null);

        // Act
        BulkPdfExportWidgetRenderer renderer = new BulkPdfExportWidgetRenderer(context);

        // Assert
        assertNotNull(renderer);
        assertEquals(1, renderer.getColumns().size());
        assertEquals(50, renderer.getTopItems());
    }

    @Test
    void testGetItemsType() {
        BulkPdfExportWidgetRenderer bulkPdfExportWidgetRenderer = mock(BulkPdfExportWidgetRenderer.class);
        when(bulkPdfExportWidgetRenderer.getItemsType(any())).thenCallRealMethod();

        assertEquals(DocumentType.LIVE_DOC, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.Document));
        assertEquals(DocumentType.LIVE_REPORT, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.RichPage));
        assertEquals(DocumentType.TEST_RUN, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.TestRun));
        assertEquals(DocumentType.BASELINE_COLLECTION, bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.BaselineCollection));
        assertThrows(IllegalArgumentException.class, () -> bulkPdfExportWidgetRenderer.getItemsType(PrototypeEnum.WorkItem));
    }

    @Test
    void testGetWidgetItemsType() {
        BulkPdfExportWidgetRenderer bulkPdfExportWidgetRenderer = mock(BulkPdfExportWidgetRenderer.class);
        when(bulkPdfExportWidgetRenderer.getWidgetItemsType(any())).thenCallRealMethod();

        assertEquals("Documents", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.Document));
        assertEquals("Pages", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.RichPage));
        assertEquals("Test Runs", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.TestRun));
        assertEquals("Collections", bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.BaselineCollection));
        assertThrows(IllegalArgumentException.class, () -> bulkPdfExportWidgetRenderer.getWidgetItemsType(PrototypeEnum.WorkItem));
    }
}
