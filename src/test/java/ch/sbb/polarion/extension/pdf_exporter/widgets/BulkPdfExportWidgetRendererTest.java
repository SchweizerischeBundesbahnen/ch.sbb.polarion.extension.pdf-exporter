package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import ch.sbb.polarion.extension.pdf_exporter.util.WidgetDescriptorSigner;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetAccessor;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldsParameterImpl;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.WidgetContextScope;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkPdfExportWidgetRendererTest {

    @Test
    void testConstructor() {
        BulkPdfExportWidgetRenderer renderer = mockRenderer(mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS));

        assertNotNull(renderer);
        assertEquals(1, renderer.getColumns().size());
        assertEquals(50, renderer.getTopItems());
    }

    @Test
    void descriptorCarriesTheResolvedDataSet() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);

        BulkExportWidgetDescriptor descriptor = renderer.buildDescriptor();

        // The query the widget resolved, not the one the page author typed: scope and subtype are folded in
        assertEquals("type:testrun AND project.id:elibrary", descriptor.getQuery());
        assertEquals("id", descriptor.getSort());
        assertEquals("BaselineCollection", descriptor.getPrototype());
        assertEquals(DocumentType.BASELINE_COLLECTION, descriptor.getDocumentType());
        assertEquals(50, descriptor.getTop());
        assertEquals("elibrary", descriptor.getProjectId());
        assertFalse(descriptor.isSqlQuery());
        assertEquals(List.of(new BulkExportColumn("elements", "elements")), descriptor.getColumns());
    }

    @Test
    void descriptorCarriesTheBaselineThePageIsOpenedIn() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        InternalReadOnlyTransaction transaction = mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS);
        when(context.transaction()).thenReturn(transaction);
        when(transaction.context().baselineRevision()).thenReturn("4711");

        assertEquals("4711", mockRenderer(context).buildDescriptor().getRevision());
    }

    @Test
    void descriptorCarriesTheCollectionOfACollectionScopedWidget() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context, WidgetContextScope.Collection, new BaselineCollectionReference("elibrary", "C1"));

        BulkExportWidgetDescriptor descriptor = renderer.buildDescriptor();

        assertEquals("elibrary", descriptor.getCollectionProjectId());
        assertEquals("C1", descriptor.getCollectionId());
    }

    @Test
    void descriptorHasNoCollectionOutsideACollectionScope() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context, WidgetContextScope.Default, new BaselineCollectionReference("elibrary", "C1"));

        BulkExportWidgetDescriptor descriptor = renderer.buildDescriptor();

        assertNull(descriptor.getCollectionProjectId());
        assertNull(descriptor.getCollectionId());
    }

    @Test
    void aCollectionScopedWidgetFallsBackToTheCollectionOfThePage() {
        // The parameter carries no reference of its own, so the one the page is opened in is used - which is
        // what Polarion's own data set accessor does
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        InternalReadOnlyTransaction transaction = mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS);
        when(context.transaction()).thenReturn(transaction);
        when(transaction.context().contextCollection()).thenReturn(new BaselineCollectionReference("library", "C7"));
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context, WidgetContextScope.Collection, null);

        BulkExportWidgetDescriptor descriptor = renderer.buildDescriptor();

        assertEquals("library", descriptor.getCollectionProjectId());
        assertEquals("C7", descriptor.getCollectionId());
    }

    @Test
    void theLoaderCarriesTheBundleVersionWhenTheManifestHasOne() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        CapturingBuilder builder = new CapturingBuilder();

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(VersionUtils::getVersion).thenReturn(Version.builder().bundleVersion("13.5.1").build());

            mockRenderer(context).render(builder.fragmentBuilder);
        }

        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(builder.scriptContent, atLeastOnce()).javaScript(script.capture());
        assertTrue(script.getValue().contains("bulk-widget.js?v=13.5.1"), script.getValue());
    }

    @Test
    void renderEmitsAShimCarryingASignedDescriptor() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context);
        CapturingBuilder builder = new CapturingBuilder();

        renderer.render(builder.fragmentBuilder);

        Map<String, String> attributes = builder.attributes();
        String descriptor = attributes.get("data-descriptor");
        assertNotNull(descriptor, "the shim must carry the descriptor");
        assertTrue(WidgetDescriptorSigner.getInstance().verify(descriptor, attributes.get("data-signature")),
                "the shim's signature must be the one this server issues for its descriptor");
        // Read by the app before the rows arrive, so that the widget's frame does not wait for the REST call
        assertEquals("Collections", attributes.get("data-title"));
        assertEquals("BASELINE_COLLECTION", attributes.get("data-document-type"));
        assertEquals("true", attributes.get("data-export-pages"));
    }

    @Test
    void renderLoadsTheWidgetAppOfThisExtensionVersion() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        CapturingBuilder builder = new CapturingBuilder();

        mockRenderer(context).render(builder.fragmentBuilder);

        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(builder.scriptContent, atLeastOnce()).javaScript(script.capture());
        String loader = script.getValue();
        assertTrue(loader.contains("/polarion/pdf-exporter-app/ui/app/assets/bulk-widget.js?v="), loader);
        // The shim's id ties the app to this widget instance: a page may carry more than one
        assertTrue(loader.matches("(?s).*module.default\\('#bulk-[0-9a-f-]+'\\).*"), loader);
    }

    @Test
    void aNegativeTopValueRendersAWarningInsteadOfTheWidget() {
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("richpages.widget.table.invalidTopValue")).thenReturn("Invalid top value");
        when(context.renderWarning("Invalid top value")).thenReturn("<div>Invalid top value</div>");
        BulkPdfExportWidgetRenderer renderer = mockRenderer(context, WidgetContextScope.Default, null, -1);
        CapturingBuilder builder = new CapturingBuilder();

        renderer.render(builder.fragmentBuilder);

        verify(builder.fragmentBuilder).html("<div>Invalid top value</div>");
        verify(builder.attributesBuilder, never()).byName(anyString(), anyString());
    }

    @Test
    void noStylesheetIsPutOnThePageForTheWidget() {
        // Everything the widget renders - the table, the export dialog and the progress dialog - lives in the shadow
        // root the widget app attaches to the shim, styled by the stylesheets that app injects into it. Four
        // stylesheets used to be inlined next to the shim for the two dialogs, which were the product's own markup in
        // the report page's body; the shim and the loader script are all that is emitted now.
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        CapturingBuilder builder = new CapturingBuilder();

        mockRenderer(context).render(builder.fragmentBuilder);

        verify(builder.tagSelector, never()).style();
    }

    @Test
    void testGetItemsType() {
        BulkPdfExportWidgetRenderer renderer = mock(BulkPdfExportWidgetRenderer.class);
        when(renderer.getItemsType(any())).thenCallRealMethod();

        assertEquals(DocumentType.LIVE_DOC, renderer.getItemsType(PrototypeEnum.Document));
        assertEquals(DocumentType.LIVE_REPORT, renderer.getItemsType(PrototypeEnum.RichPage));
        assertEquals(DocumentType.TEST_RUN, renderer.getItemsType(PrototypeEnum.TestRun));
        assertEquals(DocumentType.BASELINE_COLLECTION, renderer.getItemsType(PrototypeEnum.BaselineCollection));
        assertThrows(IllegalArgumentException.class, () -> renderer.getItemsType(PrototypeEnum.WorkItem));
    }

    @Test
    void testGetWidgetItemsType() {
        BulkPdfExportWidgetRenderer renderer = mock(BulkPdfExportWidgetRenderer.class);
        when(renderer.getWidgetItemsType(any())).thenCallRealMethod();

        assertEquals("Documents", renderer.getWidgetItemsType(PrototypeEnum.Document));
        assertEquals("Pages", renderer.getWidgetItemsType(PrototypeEnum.RichPage));
        assertEquals("Test Runs", renderer.getWidgetItemsType(PrototypeEnum.TestRun));
        assertEquals("Collections", renderer.getWidgetItemsType(PrototypeEnum.BaselineCollection));
        assertThrows(IllegalArgumentException.class, () -> renderer.getWidgetItemsType(PrototypeEnum.WorkItem));
    }

    private static BulkPdfExportWidgetRenderer mockRenderer(RichPageWidgetCommonContext context) {
        return mockRenderer(context, WidgetContextScope.Default, null);
    }

    private static BulkPdfExportWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, WidgetContextScope contextScope, BaselineCollectionReference collection) {
        return mockRenderer(context, contextScope, collection, null);
    }

    private static BulkPdfExportWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, WidgetContextScope contextScope,
                                                            BaselineCollectionReference collection, Integer top) {
        DataSetParameter dataSetParameter = mock(DataSetParameter.class);
        FieldsParameter columnsParameter = new FieldsParameterImpl.Builder("TEST").fields(List.of("elements")).build();
        SortingParameter sortingParameter = mock(SortingParameter.class);
        DataSetAccessor dataSetAccessor = mock(DataSetAccessor.class);
        DataSet dataSet = mock(DataSet.class);
        CompositeParameter advanced = mock(CompositeParameter.class);
        IntegerParameter topParameter = mock(IntegerParameter.class);
        BooleanParameter exportPages = mock(BooleanParameter.class);
        Scope scope = mock(Scope.class);

        // AbstractWidgetRenderer casts the context's transaction to the internal one
        if (context.transaction() == null || !(context.transaction() instanceof InternalReadOnlyTransaction)) {
            when(context.transaction()).thenReturn(mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS));
        }
        when(context.parameter(anyString())).thenReturn(dataSetParameter);
        doReturn(PrototypeEnum.BaselineCollection).when(dataSetParameter).prototype();
        when(dataSetParameter.get("columns")).thenReturn(columnsParameter);
        when(dataSetParameter.get("sortBy")).thenReturn(sortingParameter);
        when(dataSetParameter.get("exportPages")).thenReturn(exportPages);
        when(exportPages.value()).thenReturn(true);
        when(sortingParameter.asLuceneSortString()).thenReturn("id");
        when(dataSetParameter.scope()).thenReturn(scope);
        when(scope.projectId()).thenReturn("elibrary");
        when(dataSetParameter.getWidgetContextScope()).thenReturn(contextScope);
        when(dataSetParameter.getCollectionReference()).thenReturn(collection);

        when(dataSetParameter.getFor()).thenReturn(dataSetAccessor);
        when(dataSetAccessor.sort("id")).thenReturn(dataSetAccessor);
        when(dataSetAccessor.revision(null)).thenReturn(dataSet);
        when(dataSet.queryToShow()).thenReturn("type:testrun AND project.id:elibrary");

        when(context.parameter("advanced")).thenReturn(advanced);
        when(advanced.get("top")).thenReturn(topParameter);
        when(topParameter.value()).thenReturn(top);

        return new BulkPdfExportWidgetRenderer(context);
    }

    /**
     * Collects what the renderer writes: the attributes of the shim and the script that loads the widget app.
     * Polarion's own builders need a rendering context of their own, so they are mocked instead of instantiated.
     */
    private static class CapturingBuilder {
        private final HtmlFragmentBuilder fragmentBuilder = mock(HtmlFragmentBuilder.class, RETURNS_DEEP_STUBS);
        private final HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        private final HtmlContentBuilder scriptContent = mock(HtmlContentBuilder.class);
        @SuppressWarnings("unchecked")
        private final HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);

        private CapturingBuilder() {
            HtmlTagBuilder div = mock(HtmlTagBuilder.class, RETURNS_DEEP_STUBS);
            HtmlTagBuilder script = mock(HtmlTagBuilder.class);
            HtmlTagBuilder style = mock(HtmlTagBuilder.class, RETURNS_DEEP_STUBS);
            when(fragmentBuilder.tag()).thenReturn(tagSelector);
            when(tagSelector.style()).thenReturn(style);
            when(tagSelector.div()).thenReturn(div);
            when(div.attributes()).thenReturn(attributesBuilder);
            when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);
            when(attributesBuilder.id(anyString())).thenReturn(attributesBuilder);
            when(attributesBuilder.byName(anyString(), anyString())).thenReturn(attributesBuilder);
            // The loader is a sibling of the shim, so that the shim can become a shadow host
            when(tagSelector.script()).thenReturn(script);
            when(script.append()).thenReturn(scriptContent);
        }

        private Map<String, String> attributes() {
            ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
            verify(attributesBuilder, atLeastOnce().description("no attribute was written")).byName(names.capture(), values.capture());
            List<String> capturedNames = names.getAllValues();
            List<String> capturedValues = values.getAllValues();
            return IntStream.range(0, capturedNames.size()).boxed()
                    .collect(Collectors.toMap(capturedNames::get, capturedValues::get, (first, second) -> second));
        }
    }
}
