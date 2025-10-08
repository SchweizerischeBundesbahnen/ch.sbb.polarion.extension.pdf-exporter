package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.Renderer;
import com.polarion.alm.shared.api.model.fields.Field;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetAccessor;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.model.wi.WorkItem;
import com.polarion.alm.shared.api.model.wi.WorkItemFields;
import com.polarion.alm.shared.api.model.wi.WorkItemPermissions;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import com.polarion.alm.shared.api.utils.html.impl.HtmlBuilderConsumer;
import com.polarion.alm.shared.api.utils.html.impl.HtmlBuilderFactory;
import com.polarion.alm.shared.api.utils.html.impl.HtmlFragmentBuilderImpl;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.identification.ILocalId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BulkPdfExportWidgetRendererTest {
    @Test
    void testConstructor() {
        // Arrange & Act
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class);
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);

        // Assert
        assertNotNull(renderer);
        assertEquals(1, renderer.getColumns().size());
        assertEquals(50, renderer.getTopItems());
    }

    @Test
    void testRenderUnresolvableItem() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class);
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("richpages.widget.table.unresolvableItem", "path")).thenReturn("Unresolvable item");

        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(true);
        WorkItemReference reference = mock(WorkItemReference.class);
        when(item.getReferenceToCurrent()).thenReturn(reference);
        when(reference.toPath()).thenReturn("path");

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item);

        verify(attributesBuilder, times(1)).colspan("2");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Unresolvable item");
    }

    @Test
    void testRenderNotReadableItem() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class);
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("security.cannotread")).thenReturn("Not readable item");

        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(false);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item);

        verify(attributesBuilder, times(1)).colspan("2");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Not readable item");
    }

    @Test
    void testRenderItem() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class);

        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(true);

        IWorkItem oldApi = mock(IWorkItem.class);
        when(item.getOldApi()).thenReturn(oldApi);
        IPrototype prototype = mock(IPrototype.class);
        when(oldApi.getPrototype()).thenReturn(prototype);
        when(oldApi.getProjectId()).thenReturn("projectId");
        when(oldApi.getId()).thenReturn("objectId");
        when(prototype.getName()).thenReturn(PrototypeEnum.WorkItem.name());
        ILocalId localId = mock(ILocalId.class);
        when(oldApi.getLocalId()).thenReturn(localId);
        when(localId.getObjectName()).thenReturn("objectName");

        WorkItemFields fields = mock(WorkItemFields.class);
        when(item.fields()).thenReturn(fields);
        Field field = mock(Field.class);
        when(fields.get(anyString())).thenReturn(field);
        Renderer fieldRenderer = mock(Renderer.class);
        when(field.render()).thenReturn(fieldRenderer);
        when(fieldRenderer.withLinks(anyBoolean())).thenReturn(fieldRenderer);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);

        HtmlTagSelector<HtmlTagBuilder> tdContentTagSelector = mock(HtmlTagSelector.class);
        when(tdContentBuilder.tag()).thenReturn(tdContentTagSelector);

        HtmlTagBuilder checkboxBuilder = mock(HtmlTagBuilder.class);
        when(tdContentTagSelector.byName("input")).thenReturn(checkboxBuilder);

        HtmlAttributesBuilder checkboxAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(checkboxBuilder.attributes()).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.byName(anyString(), anyString())).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.className(anyString())).thenReturn(checkboxAttributesBuilder);

        renderer.renderItem(builder, item);

        verify(checkboxAttributesBuilder, times(1)).byName("type", "checkbox");
        verify(checkboxAttributesBuilder, times(1)).byName("data-type", PrototypeEnum.WorkItem.name());
        verify(checkboxAttributesBuilder, times(1)).byName("data-project", "projectId");
        verify(checkboxAttributesBuilder, times(1)).byName("data-id", "objectId");
        verify(checkboxAttributesBuilder, times(1)).className("export-item");
    }

    @Test
    void testRenderPanel() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        when(context.transaction()).thenReturn(mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS));

        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        when(context.parameter("dataSet")).thenReturn(dataSetParameter);
        when(dataSetParameter.get("columns")).thenReturn(mock(FieldsParameter.class, RETURNS_DEEP_STUBS));
        when(dataSetParameter.get("sortBy")).thenReturn(mock(SortingParameter.class, RETURNS_DEEP_STUBS));
        when(dataSetParameter.get("exportPages")).thenReturn(mock(BooleanParameter.class, RETURNS_DEEP_STUBS));
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.Document);

        CompositeParameter advanced = mock(CompositeParameter.class);
        when(context.parameter("advanced")).thenReturn(advanced);
        when(advanced.get("top")).thenReturn(mock(IntegerParameter.class, RETURNS_DEEP_STUBS));

        BulkPdfExportWidgetRenderer renderer = new BulkPdfExportWidgetRenderer(context);
        HtmlFragmentBuilderImpl<HtmlBuilderConsumer> builder = new HtmlFragmentBuilderImpl<>(mock(HtmlBuilderConsumer.class, RETURNS_DEEP_STUBS), mock(HtmlBuilderFactory.class, RETURNS_DEEP_STUBS));

        assertDoesNotThrow(() -> renderer.render(builder));
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

    private BulkPdfExportWidgetRenderer mockRenderer(RichPageWidgetCommonContext context) {
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

        return new BulkPdfExportWidgetRenderer(context);
    }
}
