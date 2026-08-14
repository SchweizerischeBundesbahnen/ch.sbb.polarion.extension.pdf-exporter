package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportColumn;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import ch.sbb.polarion.extension.pdf_exporter.util.WidgetDescriptorSigner;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.WidgetContextScope;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders the Bulk PDF Export widget as a shim which the React widget app mounts into.
 * <p>
 * The table itself is built in the browser: the shim carries the widget's resolved data set, and the React app asks
 * {@code /widgets/bulk-export/items} for the rows. Only what the widget rendering context alone can answer is
 * resolved here - the query, the sorting, the column labels - and it travels signed, see
 * {@link WidgetDescriptorSigner}.
 */
public class BulkPdfExportWidgetRenderer extends AbstractWidgetRenderer {

    private static final String WIDGET_MODULE_URL = "/polarion/pdf-exporter-app/ui/app/assets/bulk-widget.js";

    private final @NotNull DataSetParameter dataSetParameter;
    private final @NotNull DataSet dataSet;
    private final @Nullable String sort;

    @Getter
    private final int topItems;
    @Getter
    private final @NotNull IterableWithSize<Field> columns;
    private final @NotNull PrototypeEnum itemsPrototype;
    private final BooleanParameter exportPages;

    public BulkPdfExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
        this.dataSetParameter = context.parameter("dataSet");
        FieldsParameter columnsParameter = dataSetParameter.get("columns");
        SortingParameter sortByParameter = dataSetParameter.get("sortBy");
        String luceneSort = sortByParameter.asLuceneSortString();
        this.sort = luceneSort;
        this.dataSet = dataSetParameter.getFor().sort(luceneSort).revision(null);
        this.itemsPrototype = dataSetParameter.prototype();
        this.columns = columnsParameter.fields();
        this.exportPages = dataSetParameter.get("exportPages");

        CompositeParameter advanced = context.parameter("advanced");
        IntegerParameter top = advanced.get("top");
        Integer topValue = top.value();
        if (topValue != null) {
            this.topItems = topValue == 0 ? Integer.MAX_VALUE : topValue;
        } else {
            this.topItems = 50;
        }
    }

    public @NotNull DocumentType getItemsType(@NotNull PrototypeEnum prototype) {
        return switch (prototype) {
            case Document -> DocumentType.LIVE_DOC;
            case RichPage -> DocumentType.LIVE_REPORT;
            case TestRun -> DocumentType.TEST_RUN;
            case BaselineCollection -> DocumentType.BASELINE_COLLECTION;
            default -> throw new IllegalArgumentException("Unexpected value: " + prototype);
        };
    }

    public @NotNull String getWidgetItemsType(@NotNull PrototypeEnum prototype) {
        return switch (prototype) {
            case Document -> "Documents";
            case RichPage -> "Pages";
            case TestRun -> "Test Runs";
            case BaselineCollection -> "Collections";
            default -> throw new IllegalArgumentException("Unexpected value: " + prototype);
        };
    }

    @Override
    @VisibleForTesting
    public void render(@NotNull HtmlFragmentBuilder builder) {
        if (this.topItems < 0) {
            builder.html(this.context.renderWarning(this.localization.getString("richpages.widget.table.invalidTopValue")));
            return;
        }

        String panelId = "bulk-%s".formatted(UUID.randomUUID().toString());
        String descriptor = WidgetDescriptorSigner.getInstance().encode(buildDescriptor());

        // Nothing is put on the page for this widget. Everything it renders - the table, the export dialog and the
        // progress dialog - lives in the shadow root the widget app attaches to the shim below, styled by the
        // stylesheets that app injects into it (ui/src/widget/widget.css and ui/src/popup/export-popup.css). The
        // four stylesheets that used to be inlined here were for the two dialogs while they were the product's own
        // markup in the page body.
        HtmlTagBuilder shim = builder.tag().div();
        // sbb-ui carries generic's --sbb-* design tokens (control-tokens.css), which the widget's shadow root
        // inherits through this element: the token declarations live on .sbb-ui, while generic's
        // inputs/checkboxes rules are scoped to .form-wrapper / .standard-admin-page / .modal__container. Post-#535
        // the tokens are gone from :root, so this element is what makes the widget's tokens resolve on a plain
        // Polarion page.
        shim.attributes().className("polarion-PdfExporter-BulkExportWidget sbb-ui").id(panelId);
        shim.attributes()
                .byName("data-descriptor", descriptor)
                .byName("data-signature", WidgetDescriptorSigner.getInstance().sign(descriptor))
                // Read by the app to render the widget's frame before the rows arrive. Never trusted server-side:
                // everything the endpoint acts on comes out of the signed descriptor.
                .byName("data-title", getWidgetItemsType(itemsPrototype))
                .byName("data-document-type", getItemsType(itemsPrototype).name())
                .byName("data-export-pages", String.valueOf(exportPages.value()));

        //language=JS
        builder.tag().script().append().javaScript("""
                import('%s?v=%s')
                    .then(module => module.default('#%s'))
                    .catch(console.error);""".formatted(WIDGET_MODULE_URL, getBundleVersion(), panelId));
    }

    @VisibleForTesting
    @NotNull
    BulkExportWidgetDescriptor buildDescriptor() {
        BaselineCollectionReference collection = getCollectionReference();
        return BulkExportWidgetDescriptor.builder()
                .prototype(itemsPrototype.name())
                .documentType(getItemsType(itemsPrototype))
                .query(dataSet.queryToShow())
                .sqlQuery(DataSetQueryTypeReader.isSqlQuery(dataSetParameter))
                .sort(sort)
                .top(topItems)
                // A page opened in a baseline renders its widgets at that revision. The endpoint runs in a
                // transaction of its own, which has no baseline, so the revision has to be carried explicitly.
                .revision(context.transaction().context().baselineRevision())
                .projectId(dataSetParameter.scope().projectId())
                .collectionProjectId(collection == null ? null : collection.projectId())
                .collectionId(collection == null ? null : collection.id())
                .columns(getDescriptorColumns())
                .build();
    }

    private @NotNull List<BulkExportColumn> getDescriptorColumns() {
        List<BulkExportColumn> descriptorColumns = new ArrayList<>();
        for (Field column : columns) {
            // The label is resolved here on purpose: a field's label as the widget's column parameter knows it is
            // not reachable from the field of an item, which is all the endpoint has.
            descriptorColumns.add(new BulkExportColumn(column.id(), column.label()));
        }
        return descriptorColumns;
    }

    /**
     * The collection a page opened inside a baseline collection queries, which Polarion takes from the widget's own
     * parameter first and from the page context second.
     */
    private @Nullable BaselineCollectionReference getCollectionReference() {
        if (dataSetParameter.getWidgetContextScope() != WidgetContextScope.Collection) {
            return null;
        }
        BaselineCollectionReference reference = dataSetParameter.getCollectionReference();
        return reference != null ? reference : context.transaction().context().contextCollection();
    }

    /**
     * Busts the browser cache of the widget app when the extension is updated: the app is loaded from a fixed URL,
     * as the renderer cannot know the hashed file names Vite emits for the rest of the bundle.
     */
    private @NotNull String getBundleVersion() {
        String version = VersionUtils.getVersion().getBundleVersion();
        return version == null ? "0" : version;
    }
}
