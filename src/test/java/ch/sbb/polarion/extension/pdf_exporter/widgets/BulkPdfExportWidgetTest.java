package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetDependenciesContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BulkPdfExportWidgetTest {

    @Test
    void testGetIcon() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetContext context = mock(RichPageWidgetContext.class);

        String icon = widget.getIcon(context);

        assertEquals("/polarion/pdf-exporter-admin/ui/images/app-icon.svg", icon);
    }

    @Test
    void testGetTags() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        SharedContext context = mock(SharedContext.class);

        Iterable<String> tags = widget.getTags(context);

        assertNotNull(tags);
        assertTrue(tags.iterator().hasNext());
        assertEquals("PDF Export", tags.iterator().next());
    }

    @Test
    void testGetLabel() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        SharedContext context = mock(SharedContext.class);

        String label = widget.getLabel(context);

        assertEquals("Bulk PDF Export", label);
    }

    @Test
    void testGetDetailsHtml() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetContext context = mock(RichPageWidgetContext.class);

        String details = widget.getDetailsHtml(context);

        assertEquals("Renders a widget providing functionality of exporting multiple documents, reports and test runs to PDF in a single run", details);
    }

    @Test
    void testGetParametersDefinition() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();

        ParameterFactory parameterFactory = mock(ParameterFactory.class, RETURNS_DEEP_STUBS);
        SharedLocalization localization = mock(SharedLocalization.class);
        SharedContext context = mock(SharedContext.class);

        when(parameterFactory.context()).thenReturn(context);
        when(context.localization()).thenReturn(localization);
        when(localization.getString(anyString())).thenReturn("test");

        FieldsParameter.Builder fieldsBuilder = mock(FieldsParameter.Builder.class, RETURNS_DEEP_STUBS);
        FieldsParameter fieldsParameter = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);
        when(parameterFactory.fields(anyString())).thenReturn(fieldsBuilder);
        when(fieldsBuilder.build()).thenReturn(fieldsParameter);
        when(fieldsBuilder.disallowedFields(any())).thenReturn(fieldsBuilder);
        when(fieldsBuilder.dependencyTarget(anyBoolean())).thenReturn(fieldsBuilder);

        SortingParameter.Builder sortingBuilder = mock(SortingParameter.Builder.class, RETURNS_DEEP_STUBS);
        SortingParameter sortingParameter = mock(SortingParameter.class);
        when(parameterFactory.sorting(anyString())).thenReturn(sortingBuilder);
        when(sortingBuilder.build()).thenReturn(sortingParameter);

        BooleanParameter.Builder boolBuilder = mock(BooleanParameter.Builder.class, RETURNS_DEEP_STUBS);
        BooleanParameter boolParameter = mock(BooleanParameter.class);
        when(parameterFactory.bool(anyString())).thenReturn(boolBuilder);
        when(boolBuilder.dependencyTarget(anyBoolean())).thenReturn(boolBuilder);
        when(boolBuilder.value(anyBoolean())).thenReturn(boolBuilder);
        when(boolBuilder.build()).thenReturn(boolParameter);

        DataSetParameter.Builder dataSetBuilder = mock(DataSetParameter.Builder.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class);
        when(parameterFactory.dataSet(anyString())).thenReturn(dataSetBuilder);
        when(dataSetBuilder.allowedPrototypes(any())).thenReturn(dataSetBuilder);
        when(dataSetBuilder.add(anyString(), any(RichPageParameter.class))).thenReturn(dataSetBuilder);
        when(dataSetBuilder.dependencySource(anyBoolean())).thenReturn(dataSetBuilder);
        when(dataSetBuilder.build()).thenReturn(dataSetParameter);

        IntegerParameter.Builder integerBuilder = mock(IntegerParameter.Builder.class, RETURNS_DEEP_STUBS);
        IntegerParameter integerParameter = mock(IntegerParameter.class);
        when(parameterFactory.integer(anyString())).thenReturn(integerBuilder);
        when(integerBuilder.value(anyInt())).thenReturn(integerBuilder);
        when(integerBuilder.build()).thenReturn(integerParameter);

        CompositeParameter.Builder compositeBuilder = mock(CompositeParameter.Builder.class, RETURNS_DEEP_STUBS);
        CompositeParameter compositeParameter = mock(CompositeParameter.class);
        when(parameterFactory.composite(anyString())).thenReturn(compositeBuilder);
        when(compositeBuilder.collapsedByDefault(anyBoolean())).thenReturn(compositeBuilder);
        when(compositeBuilder.add(anyString(), any(RichPageParameter.class))).thenReturn(compositeBuilder);
        when(compositeBuilder.build()).thenReturn(compositeParameter);

        ReadOnlyStrictMap<String, RichPageParameter> parameters = widget.getParametersDefinition(parameterFactory);

        assertNotNull(parameters);
        assertTrue(parameters.containsKey("dataSet"));
        assertTrue(parameters.containsKey("propertiesSidebarFields"));
        assertTrue(parameters.containsKey("advanced"));

        verify(parameterFactory, times(2)).fields(anyString());
        verify(parameterFactory).sorting(anyString());
        verify(parameterFactory).bool(anyString());
        verify(parameterFactory).dataSet(anyString());
        verify(parameterFactory).integer(anyString());
        verify(parameterFactory).composite(anyString());
        verify(dataSetBuilder).allowedPrototypes(PrototypeEnum.Document, PrototypeEnum.TestRun, PrototypeEnum.RichPage, PrototypeEnum.BaselineCollection);
        verify(integerBuilder).value(50);
    }

    @Test
    void testRenderHtml() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetRenderingContext renderingContext = mock(RichPageWidgetRenderingContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        FieldsParameter columnsParameter = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);
        SortingParameter sortByParameter = mock(SortingParameter.class, RETURNS_DEEP_STUBS);
        BooleanParameter exportPagesParameter = mock(BooleanParameter.class, RETURNS_DEEP_STUBS);
        CompositeParameter compositeParameter = mock(CompositeParameter.class, RETURNS_DEEP_STUBS);
        IntegerParameter topParameter = mock(IntegerParameter.class, RETURNS_DEEP_STUBS);
        InternalReadOnlyTransaction transaction = mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS);

        when(renderingContext.transaction()).thenReturn(transaction);
        when(renderingContext.parameter("dataSet")).thenReturn(dataSetParameter);
        when(renderingContext.parameter("advanced")).thenReturn(compositeParameter);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.Document);
        when(dataSetParameter.get("columns")).thenReturn(columnsParameter);
        when(dataSetParameter.get("sortBy")).thenReturn(sortByParameter);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPagesParameter);
        when(compositeParameter.get("top")).thenReturn(topParameter);

        String html = widget.renderHtml(renderingContext);

        assertNotNull(html);
    }

    @Test
    void testProcessParameterDependencies_BaselineCollection() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetDependenciesContext context = mock(RichPageWidgetDependenciesContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        BooleanParameter exportPagesParameter = mock(BooleanParameter.class, RETURNS_DEEP_STUBS);
        RichPageParameter.Visuals visuals = mock(RichPageParameter.Visuals.class);
        FieldsParameter propertiesSidebarFields = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);

        when(context.parameter("dataSet")).thenReturn(dataSetParameter);
        when(context.parameter("propertiesSidebarFields")).thenReturn(propertiesSidebarFields);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.BaselineCollection);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPagesParameter);
        when(exportPagesParameter.visuals()).thenReturn(visuals);

        widget.processParameterDependencies(context);

        verify(visuals).setVisible(true);
    }

    @Test
    void testProcessParameterDependencies_Document() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetDependenciesContext context = mock(RichPageWidgetDependenciesContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        BooleanParameter exportPagesParameter = mock(BooleanParameter.class, RETURNS_DEEP_STUBS);
        RichPageParameter.Visuals visuals = mock(RichPageParameter.Visuals.class);
        FieldsParameter propertiesSidebarFields = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);

        when(context.parameter("dataSet")).thenReturn(dataSetParameter);
        when(context.parameter("propertiesSidebarFields")).thenReturn(propertiesSidebarFields);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.Document);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPagesParameter);
        when(exportPagesParameter.visuals()).thenReturn(visuals);

        widget.processParameterDependencies(context);

        verify(visuals).setVisible(false);
    }

    @Test
    void testProcessParameterDependencies_TestRun() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetDependenciesContext context = mock(RichPageWidgetDependenciesContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        BooleanParameter exportPagesParameter = mock(BooleanParameter.class, RETURNS_DEEP_STUBS);
        RichPageParameter.Visuals visuals = mock(RichPageParameter.Visuals.class);
        FieldsParameter propertiesSidebarFields = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);

        when(context.parameter("dataSet")).thenReturn(dataSetParameter);
        when(context.parameter("propertiesSidebarFields")).thenReturn(propertiesSidebarFields);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.TestRun);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPagesParameter);
        when(exportPagesParameter.visuals()).thenReturn(visuals);

        widget.processParameterDependencies(context);

        verify(visuals).setVisible(false);
    }

    @Test
    void testProcessParameterDependencies_RichPage() {
        BulkPdfExportWidget widget = new BulkPdfExportWidget();
        RichPageWidgetDependenciesContext context = mock(RichPageWidgetDependenciesContext.class, RETURNS_DEEP_STUBS);
        DataSetParameter dataSetParameter = mock(DataSetParameter.class, RETURNS_DEEP_STUBS);
        BooleanParameter exportPagesParameter = mock(BooleanParameter.class, RETURNS_DEEP_STUBS);
        RichPageParameter.Visuals visuals = mock(RichPageParameter.Visuals.class);
        FieldsParameter propertiesSidebarFields = mock(FieldsParameter.class, RETURNS_DEEP_STUBS);

        when(context.parameter("dataSet")).thenReturn(dataSetParameter);
        when(context.parameter("propertiesSidebarFields")).thenReturn(propertiesSidebarFields);
        when(dataSetParameter.prototype()).thenReturn(PrototypeEnum.RichPage);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPagesParameter);
        when(exportPagesParameter.visuals()).thenReturn(visuals);

        widget.processParameterDependencies(context);

        verify(visuals).setVisible(false);
    }
}
