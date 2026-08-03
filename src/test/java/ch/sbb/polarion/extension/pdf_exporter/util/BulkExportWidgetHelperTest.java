package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItem;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.ModelObjectsBase;
import com.polarion.alm.shared.api.model.ModelObjectsSearch;
import com.polarion.alm.shared.api.model.ModelObjectsSqlSearch;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.Renderer;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.fields.Field;
import com.polarion.alm.shared.api.model.fields.Fields;
import com.polarion.alm.shared.api.model.ModelObjectPermissions;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlBuilderTargetSelector;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.platform.persistence.model.IPrototype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BulkExportWidgetHelperTest {

    private final BulkExportWidgetHelper helper = new BulkExportWidgetHelper();
    private ReadOnlyTransaction transaction;

    @BeforeEach
    void setUp() {
        transaction = mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS);
        when(transaction.context().localization().getString(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(0) + ":" + invocation.getArgument(1));
    }

    @Test
    void luceneQueryIsExecutedForThePrototypeOfTheDescriptor() {
        ModelObjectsSearch<ModelObject> search = search(testRun("elibrary", "TR-1"));
        ModelObjectsBase<ModelObject, ?> objects = mock(ModelObjectsBase.class);
        doReturn(search).when(objects).search();
        doReturn(objects).when(transaction).byEnum(PrototypeEnum.TestRun);

        BulkExportItems result = helper.getItems(descriptor().sqlQuery(false).query("status:passed").sort("id").revision("42").build(), transaction);

        verify(search).query("status:passed");
        verify(search).sort("id");
        verify(search).baseline("42");
        assertEquals(1, result.getItems().size());
        assertEquals("TR-1", result.getItems().getFirst().getId());
        assertEquals("elibrary", result.getItems().getFirst().getProjectId());
        assertEquals("TestRun", result.getItems().getFirst().getType());
    }

    @Test
    void sqlQueryIsExecutedThroughTheSqlSearch() {
        ModelObjectsSqlSearch sqlSearch = mock(ModelObjectsSqlSearch.class);
        when(sqlSearch.baseline(any())).thenReturn(sqlSearch);
        when(sqlSearch.iterator()).thenReturn(List.<ModelObject>of().iterator());
        when(transaction.objects().searchBySql("SELECT 1")).thenReturn(sqlSearch);

        helper.getItems(descriptor().sqlQuery(true).query("SELECT 1").build(), transaction);

        verify(transaction.objects()).searchBySql("SELECT 1");
        verify(sqlSearch).baseline(null);
    }

    @Test
    void sqlQueryOfACollectionScopedWidgetSearchesInThatCollection() {
        ModelObjectsSqlSearch sqlSearch = mock(ModelObjectsSqlSearch.class);
        when(sqlSearch.collection(any())).thenReturn(sqlSearch);
        when(sqlSearch.iterator()).thenReturn(List.<ModelObject>of().iterator());
        when(transaction.objects().searchBySql(anyString())).thenReturn(sqlSearch);

        helper.getItems(descriptor().sqlQuery(true).query("SELECT 1").collectionProjectId("elibrary").collectionId("C1").build(), transaction);

        ArgumentCaptor<BaselineCollectionReference> captor = ArgumentCaptor.forClass(BaselineCollectionReference.class);
        verify(sqlSearch).collection(captor.capture());
        assertEquals("elibrary", captor.getValue().projectId());
        assertEquals("C1", captor.getValue().id());
    }

    @Test
    void cellsAreRenderedForEveryColumnOfTheDescriptor() {
        ModelObject item = testRun("elibrary", "TR-1");
        Fields fields = mock(Fields.class);
        Field idField = field("<a>TR-1</a>");
        Field statusField = field("<span>passed</span>");
        when(item.fields()).thenReturn(fields);
        when(fields.get("id")).thenReturn(idField);
        when(fields.get("status")).thenReturn(statusField);
        stubSearch(item);

        BulkExportItems result = helper.getItems(
                descriptor().columns(List.of(new BulkExportColumn("id", "ID"), new BulkExportColumn("status", "Status"))).build(), transaction);

        assertEquals(List.of("<a>TR-1</a>", "<span>passed</span>"), result.getItems().getFirst().getCells());
        assertEquals(List.of(new BulkExportColumn("id", "ID"), new BulkExportColumn("status", "Status")), result.getColumns());
    }

    @Test
    void documentsCarryTheirSpace() {
        ModelObject document = mock(ModelObject.class);
        IModule module = mock(IModule.class);
        IPrototype modulePrototype = prototype("Module");
        doReturn(module).when(document).getOldApi();
        when(module.getPrototype()).thenReturn(modulePrototype);
        when(module.getProjectId()).thenReturn("elibrary");
        when(module.getId()).thenReturn("Specification");
        when(module.getModuleFolder()).thenReturn("Requirements");
        doReturn(permissions(true)).when(document).can();
        stubSearch(document);

        BulkExportItems result = helper.getItems(descriptor().build(), transaction);

        assertEquals("Requirements", result.getItems().getFirst().getSpaceId());
        assertEquals("Specification", result.getItems().getFirst().getId());
    }

    @Test
    void anUnreadableItemBecomesAMessageRow() {
        ModelObject item = mock(ModelObject.class);
        when(item.isUnresolvable()).thenReturn(false);
        doReturn(permissions(false)).when(item).can();
        when(transaction.context().localization().getString("security.cannotread")).thenReturn("No permission");
        stubSearch(item);

        BulkExportItem row = helper.getItems(descriptor().build(), transaction).getItems().getFirst();

        assertFalse(row.isReadable());
        assertEquals("No permission", row.getMessage());
        assertNull(row.getId());
        assertNull(row.getCells());
    }

    @Test
    void anUnresolvableItemBecomesAMessageRow() {
        ModelObject item = mock(ModelObject.class);
        when(item.isUnresolvable()).thenReturn(true);
        WorkItemReference reference = mock(WorkItemReference.class);
        when(reference.toPath()).thenReturn("elibrary/GONE-1");
        when(item.getReferenceToCurrent()).thenReturn(reference);
        stubSearch(item);

        BulkExportItem row = helper.getItems(descriptor().build(), transaction).getItems().getFirst();

        assertFalse(row.isReadable());
        assertEquals("richpages.widget.table.unresolvableItem:elibrary/GONE-1", row.getMessage());
    }

    @Test
    void noMoreRowsThanTheTopValueAreReturnedAndTheFooterReportsTheRest() {
        when(transaction.context().localization().getString(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1) + " of " + invocation.getArgument(2));
        stubSearch(testRun("elibrary", "TR-1"), testRun("elibrary", "TR-2"), testRun("elibrary", "TR-3"));

        BulkExportItems result = helper.getItems(descriptor().top(2).build(), transaction);

        assertEquals(2, result.getItems().size());
        assertEquals(3, result.getTotalCount());
        assertEquals("2 of 3", result.getCountMessage());
    }

    @Test
    void theFooterReportsTheTotalWhenEverythingIsShown() {
        stubSearch(testRun("elibrary", "TR-1"));

        BulkExportItems result = helper.getItems(descriptor().top(50).build(), transaction);

        assertEquals("form.modules.label.showMulti.item:1", result.getCountMessage());
    }

    @Test
    void onlyTestRunsGetAnOpenInTableLink() {
        when(transaction.context().createPortalLink().project("elibrary").testRuns().query("status:passed").toEncodedRelativeUrl())
                .thenReturn("/polarion/#/project/elibrary/testruns");
        stubSearch();

        assertEquals("/polarion/#/project/elibrary/testruns",
                helper.getItems(descriptor().prototype("TestRun").projectId("elibrary").query("status:passed").build(), transaction).getOpenInTableUrl());
        assertNull(helper.getItems(descriptor().prototype("Document").projectId("elibrary").query("status:passed").build(), transaction).getOpenInTableUrl());
        assertNull(helper.getItems(descriptor().prototype("TestRun").projectId(null).query("status:passed").build(), transaction).getOpenInTableUrl());
        // A SQL data set has no table view to open either
        assertNull(helper.getItems(descriptor().prototype("TestRun").projectId("elibrary").sqlQuery(true).query("SELECT 1").build(), transaction).getOpenInTableUrl());
    }

    @Test
    void pagesCarryTheirSpaceAndCollectionsTheirName() {
        ModelObject page = mock(ModelObject.class);
        IRichPage richPage = mock(IRichPage.class);
        IPrototype pagePrototype = prototype("RichPage");
        doReturn(richPage).when(page).getOldApi();
        when(richPage.getPrototype()).thenReturn(pagePrototype);
        when(richPage.getProjectId()).thenReturn("elibrary");
        when(richPage.getId()).thenReturn("Bulk PDF");
        when(richPage.getSpaceId()).thenReturn("Reports");
        doReturn(permissions(true)).when(page).can();

        ModelObject collection = mock(ModelObject.class);
        IBaselineCollection baselineCollection = mock(IBaselineCollection.class);
        IPrototype collectionPrototype = prototype("BaselineCollection");
        doReturn(baselineCollection).when(collection).getOldApi();
        when(baselineCollection.getPrototype()).thenReturn(collectionPrototype);
        when(baselineCollection.getProjectId()).thenReturn("elibrary");
        when(baselineCollection.getId()).thenReturn("C1");
        when(baselineCollection.getName()).thenReturn("Release 1.0");
        doReturn(permissions(true)).when(collection).can();

        stubSearch(page, collection);

        List<BulkExportItem> items = helper.getItems(descriptor().build(), transaction).getItems();
        assertEquals("Reports", items.getFirst().getSpaceId());
        assertNull(items.getFirst().getName());
        // A collection is addressed by its name, and has no space of its own
        assertEquals("Release 1.0", items.get(1).getName());
        assertNull(items.get(1).getSpaceId());
    }

    @Test
    void aWidgetWithoutColumnsRendersNoCells() {
        stubSearch(testRun("elibrary", "TR-1"));

        BulkExportItems result = helper.getItems(descriptor().columns(null).build(), transaction);

        assertEquals(List.of(), result.getColumns());
        assertEquals(List.of(), result.getItems().getFirst().getCells());
    }

    @Test
    void aHalfSpecifiedCollectionReferenceIsIgnored() {
        ModelObjectsSqlSearch sqlSearch = mock(ModelObjectsSqlSearch.class);
        when(sqlSearch.baseline(any())).thenReturn(sqlSearch);
        when(sqlSearch.iterator()).thenReturn(List.<ModelObject>of().iterator());
        when(transaction.objects().searchBySql(anyString())).thenReturn(sqlSearch);

        // Only half a reference is no reference: the search runs against the baseline instead
        helper.getItems(descriptor().sqlQuery(true).query("SELECT 1").collectionProjectId("elibrary").build(), transaction);

        verify(sqlSearch).baseline(null);
        verify(sqlSearch, never()).collection(any());
    }

    @Test
    void theQueryIsReportedBackForTheFooterInfo() {
        stubSearch();

        assertEquals("status:passed", helper.getItems(descriptor().query("status:passed").build(), transaction).getQuery());
    }

    private static BulkExportWidgetDescriptor.BulkExportWidgetDescriptorBuilder descriptor() {
        return BulkExportWidgetDescriptor.builder()
                .prototype("TestRun")
                .top(50)
                .columns(List.of());
    }

    /**
     * Polarion's search API is generic over the prototype, and its wildcards do not survive mocking - hence
     * doReturn over when().thenReturn(), which is what lets the stubs be assigned at all.
     */
    private void stubSearch(ModelObject... items) {
        ModelObjectsSearch<ModelObject> search = search(items);
        ModelObjectsBase<ModelObject, ?> objects = mock(ModelObjectsBase.class);
        doReturn(search).when(objects).search();
        doReturn(objects).when(transaction).byEnum(any());
    }

    @SuppressWarnings("unchecked")
    private static ModelObjectsSearch<ModelObject> search(ModelObject... items) {
        ModelObjectsSearch<ModelObject> search = mock(ModelObjectsSearch.class);
        when(search.query(any())).thenReturn(search);
        when(search.sort(any())).thenReturn(search);
        when(search.baseline(any())).thenReturn(search);
        when(search.size()).thenReturn(items.length);
        when(search.iterator()).thenAnswer(invocation -> List.of(items).iterator());
        return search;
    }

    private static ModelObject testRun(String projectId, String id) {
        ModelObject item = mock(ModelObject.class);
        ITestRun testRun = mock(ITestRun.class);
        IPrototype prototype = prototype("TestRun");
        doReturn(testRun).when(item).getOldApi();
        when(testRun.getPrototype()).thenReturn(prototype);
        when(testRun.getProjectId()).thenReturn(projectId);
        when(testRun.getId()).thenReturn(id);
        doReturn(permissions(true)).when(item).can();
        return item;
    }

    private static IPrototype prototype(String name) {
        IPrototype prototype = mock(IPrototype.class);
        when(prototype.getName()).thenReturn(name);
        return prototype;
    }

    private static ModelObjectPermissions permissions(boolean read) {
        ModelObjectPermissions permissions = mock(ModelObjectPermissions.class);
        when(permissions.read()).thenReturn(read);
        return permissions;
    }

    /**
     * A field whose renderer answers the given HTML for the gwt target - the one a rich page renders into.
     * Renderer is self-referentially generic (`Renderer&lt;? extends Renderer&lt;?&gt;&gt;`), which a mock cannot
     * express, so the raw type is deliberate here.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Field field(String html) {
        Field field = mock(Field.class);
        Renderer renderer = mock(Renderer.class);
        when(field.render()).thenReturn(renderer);
        when(renderer.withLinks(anyBoolean())).thenReturn(renderer);
        HtmlBuilderTargetSelector<String> selector = mock(HtmlBuilderTargetSelector.class);
        when(renderer.htmlFor()).thenReturn(selector);
        when(selector.gwt()).thenReturn(html);
        return field;
    }

}
