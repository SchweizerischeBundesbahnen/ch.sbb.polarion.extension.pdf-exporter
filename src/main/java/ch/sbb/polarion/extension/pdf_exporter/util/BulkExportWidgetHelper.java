package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItem;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.ModelObjectsBase;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.links.PortalLink;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Executes the data set of a Bulk PDF Export widget outside the widget rendering context.
 * <p>
 * The widget renders as a React shim which asks for its rows over REST, so the query the rich page resolved at
 * render time has to run again here. This mirrors what Polarion's own {@code DataSetAccessorImpl} does with the
 * resolved query, and renders the cells with the same {@code gwt} HTML target the rich page uses, so the table
 * looks exactly as it did when the renderer built it tag by tag.
 */
public class BulkExportWidgetHelper {

    public @NotNull BulkExportItems getItems(@NotNull BulkExportWidgetDescriptor descriptor, @NotNull ReadOnlyTransaction transaction) {
        IterableWithSize<? extends ModelObject> found = search(descriptor, transaction);
        List<BulkExportColumn> columns = descriptor.getColumns() == null ? List.of() : descriptor.getColumns();

        List<BulkExportItem> items = new ArrayList<>();
        int count = 0;
        for (Iterator<? extends ModelObject> iterator = found.iterator(); iterator.hasNext() && count < descriptor.getTop(); count++) {
            items.add(toItem(iterator.next(), columns, transaction));
        }

        PortalLink openInTableLink = createOpenInTableLink(descriptor, transaction);
        return BulkExportItems.builder()
                .columns(columns)
                .items(items)
                .totalCount(found.size())
                .countMessage(getCountMessage(found.size(), descriptor.getTop(), transaction))
                .openInTableUrl(openInTableLink == null ? null : openInTableLink.toEncodedRelativeUrl())
                .query(descriptor.getQuery())
                .build();
    }

    private @NotNull IterableWithSize<? extends ModelObject> search(@NotNull BulkExportWidgetDescriptor descriptor, @NotNull ReadOnlyTransaction transaction) {
        if (descriptor.isSqlQuery()) {
            BaselineCollectionReference collection = getCollectionReference(descriptor);
            return collection != null
                    ? transaction.objects().searchBySql(descriptor.getQuery()).collection(collection)
                    : transaction.objects().searchBySql(descriptor.getQuery()).baseline(descriptor.getRevision());
        }
        ModelObjectsBase<? extends ModelObject, ?> objects = transaction.byEnum(PrototypeEnum.valueOf(descriptor.getPrototype()));
        return objects.search().query(descriptor.getQuery()).sort(descriptor.getSort()).baseline(descriptor.getRevision());
    }

    private @Nullable BaselineCollectionReference getCollectionReference(@NotNull BulkExportWidgetDescriptor descriptor) {
        if (descriptor.getCollectionProjectId() == null || descriptor.getCollectionId() == null) {
            return null;
        }
        return new BaselineCollectionReference(descriptor.getCollectionProjectId(), descriptor.getCollectionId());
    }

    private @NotNull BulkExportItem toItem(@NotNull ModelObject item, @NotNull List<BulkExportColumn> columns, @NotNull ReadOnlyTransaction transaction) {
        if (item.isUnresolvable()) {
            return notReadable(transaction.context().localization().getString("richpages.widget.table.unresolvableItem", item.getReferenceToCurrent().toPath()));
        }
        if (!item.can().read()) {
            return notReadable(transaction.context().localization().getString("security.cannotread"));
        }

        List<String> cells = new ArrayList<>();
        for (BulkExportColumn column : columns) {
            // The rich page renders its widgets into the gwt target (see RichPageContextImpl), so the same one is
            // used here: another target produces different links and icon markup for the very same field.
            cells.add(item.fields().get(column.getId()).render().withLinks(true).htmlFor().gwt());
        }

        IUniqueObject oldApi = (IUniqueObject) item.getOldApi();
        return BulkExportItem.builder()
                .readable(true)
                .type(item.getOldApi().getPrototype().getName())
                .projectId(oldApi.getProjectId())
                .spaceId(getSpaceId(item))
                .id(oldApi.getId())
                .name(getObjectName(item))
                .cells(cells)
                .build();
    }

    private @NotNull BulkExportItem notReadable(@NotNull String message) {
        return BulkExportItem.builder().readable(false).message(message).build();
    }

    private @Nullable String getSpaceId(@NotNull ModelObject item) {
        if (item.getOldApi() instanceof IModule module) {
            return module.getModuleFolder();
        }
        if (item.getOldApi() instanceof IRichPage richPage) {
            return richPage.getSpaceId();
        }
        return null;
    }

    private @Nullable String getObjectName(@NotNull ModelObject item) {
        if (item.getOldApi() instanceof IBaselineCollection baselineCollection) {
            return baselineCollection.getName();
        }
        return null;
    }

    private @NotNull String getCountMessage(int size, int top, @NotNull ReadOnlyTransaction transaction) {
        return size <= top
                ? transaction.context().localization().getString("form.modules.label.showMulti.item", String.valueOf(size))
                : transaction.context().localization().getString("form.modules.label.showMultiOf.item", String.valueOf(top), String.valueOf(size));
    }

    /**
     * Only test runs have a table view to open, exactly as in Polarion's {@code BottomQueryLinksBuilder}: documents,
     * pages and collections get no link there either. The query carries its scope already, which makes the project
     * clause redundant in the link but not wrong.
     */
    private @Nullable PortalLink createOpenInTableLink(@NotNull BulkExportWidgetDescriptor descriptor, @NotNull ReadOnlyTransaction transaction) {
        if (descriptor.isSqlQuery() || descriptor.getProjectId() == null || !PrototypeEnum.TestRun.name().equals(descriptor.getPrototype())) {
            return null;
        }
        return transaction.context().createPortalLink().project(descriptor.getProjectId()).testRuns().query(descriptor.getQuery());
    }
}
