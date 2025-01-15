package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.BottomQueryLinksBuilder;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.Field;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.ui.shared.LinearGradientColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class BulkPdfExportWidgetRenderer extends AbstractWidgetRenderer {
    private final @NotNull DataSet dataSet;

    private final @NotNull IterableWithSize<ModelObject> items;
    @Getter
    private final int topItems;
    @Getter
    private final @NotNull IterableWithSize<Field> columns;
    private final @NotNull PrototypeEnum itemsPrototype;

    public BulkPdfExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
        DataSetParameter dataSetParameter = context.parameter("dataSet");
        FieldsParameter columnsParameter = dataSetParameter.get("columns");
        SortingParameter sortByParameter = dataSetParameter.get("sortBy");
        String sort = sortByParameter.asLuceneSortString();
        this.dataSet = dataSetParameter.getFor().sort(sort).revision(null);
        this.items = this.dataSet.items();
        itemsPrototype = dataSetParameter.prototype();
        this.columns = columnsParameter.fields();

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

    protected void render(@NotNull HtmlFragmentBuilder builder) {
        if (this.topItems < 0) {
            builder.html(this.context.renderWarning(this.localization.getString("richpages.widget.table.invalidTopValue")));
        } else {
            HtmlTagBuilder wrap = builder.tag().div();
            wrap.attributes().className("polarion-PdfExporter-BulkExportWidget");

            HtmlTagBuilder header = wrap.append().tag().div();
            header.attributes().className("header");
            header.attributes().byName("document-type", getItemsType(itemsPrototype).name());

            HtmlTagBuilder title = header.append().tag().h3();
            title.append().text(getWidgetItemsType(itemsPrototype));

            renderExportButton(header);

            HtmlTagBuilder description = header.append().tag().div();
            description.append().tag().p().append().text(String.format("Please select %s below which you want to export and click button above", getWidgetItemsType(itemsPrototype)));

            HtmlTagBuilder exportItems = wrap.append().tag().div();
            exportItems.attributes().className("export-items");

            HtmlTagBuilder mainTable = exportItems.append().tag().table();

            mainTable.attributes().className("polarion-rpw-table-main");
            //language=JS
            mainTable.attributes().onClick("BulkPdfExporter.updateBulkExportButton(this);");

            HtmlContentBuilder contentBuilder = mainTable.append();
            this.renderContentTable(contentBuilder.tag().tr().append().tag().td().append());
            this.renderFooter(contentBuilder.tag().tr().append().tag().td());
        }
    }

    private void renderExportButton(@NotNull HtmlTagBuilder header) {
        LinearGradientColor color = new LinearGradientColor();
        HtmlTagBuilder buttonSpan = header.append().tag().span();
        buttonSpan.attributes().className("polarion-TestsExecutionButton-link").title("Please, select at least one item to be exported first");

        HtmlTagBuilder a = buttonSpan.append().tag().a();
        HtmlTagBuilder button = a.append().tag().div();
        button.attributes().className("polarion-TestsExecutionButton-buttons polarion-TestsExecutionButton-buttons-defaultCursor").style(color.getStyle());

        HtmlTagBuilder content = button.append().tag().table();
        content.attributes().className("polarion-TestsExecutionButton-buttons-content");

        HtmlTagBuilder labelCell = content.append().tag().tr().append().tag().td();
        labelCell.attributes().className("polarion-TestsExecutionButton-buttons-content-labelCell");
        HtmlTagBuilder label = labelCell.append().tag().div();
        label.attributes().className("polarion-TestsExecutionButton-labelTextNew");

        label.append().text("Export to PDF");
    }

    private void renderContentTable(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder table = builder.tag().table();
        table.attributes().className("polarion-rpw-table-content");
        this.renderContentRows(table.append());
    }

    private void renderContentRows(@NotNull HtmlContentBuilder builder) {
        this.renderHeaderRow(builder);
        int count = 0;

        for (Iterator<ModelObject> iterator = this.items.iterator(); iterator.hasNext(); ++count) {
            if (count >= this.topItems) {
                break;
            }

            HtmlTagBuilder tr = builder.tag().tr();
            tr.attributes().className("polarion-rpw-table-content-row");
            this.renderItem(tr.append(), iterator.next());
        }

    }

    public void renderItem(@NotNull HtmlContentBuilder builder, @NotNull ModelObject item) {
        if (item.isUnresolvable()) {
            this.renderNotReadableRow(builder, this.localization.getString("richpages.widget.table.unresolvableItem", item.getReferenceToCurrent().toPath()));
        } else if (!item.can().read()) {
            this.renderNotReadableRow(builder, this.localization.getString("security.cannotread"));
        } else {
            HtmlTagBuilder td = builder.tag().td();
            HtmlTagBuilder checkbox = td.append().tag().byName("input");
            checkbox.attributes()
                    .byName("type", "checkbox")
                    .byName("data-type", item.getOldApi().getPrototype().getName())
                    .byName("data-project", getProjectId(item))
                    .byName("data-space", getSpace(item))
                    .byName("data-id", getObjectId(item))
                    .className("export-item");

            if (PrototypeEnum.BaselineCollection.name().equals(item.getOldApi().getPrototype().getName())) {
                checkbox.attributes().byName("data-name", getObjectName(item));
            }

            for (Field column : this.columns) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
    }

    private @NotNull String getSpace(@NotNull ModelObject item) {
        if (item.getOldApi() instanceof IModule module) {
            return module.getModuleFolder();
        }
        if (item.getOldApi() instanceof IRichPage richPage) {
            return richPage.getSpaceId();
        }
        return "";
    }

    private String getProjectId(@NotNull ModelObject item) {
        return ((IUniqueObject) item.getOldApi()).getProjectId();
    }

    private String getObjectName(@NotNull ModelObject item) {
        return item.getOldApi().getLocalId().getObjectName();
    }

    private String getObjectId(@NotNull ModelObject item) {
        return ((IUniqueObject) item.getOldApi()).getId();
    }

    private void renderNotReadableRow(@NotNull HtmlContentBuilder builder, @NotNull String message) {
        HtmlTagBuilder td = builder.tag().td();
        td.attributes().colspan("" + (this.columns.size() + 1)).className("polarion-rpw-table-not-readable-cell");
        td.append().text(message);
    }

    private void renderHeaderRow(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder row = builder.tag().tr();
        row.attributes().className("polarion-rpw-table-header-row");
        HtmlTagBuilder th = row.append().tag().th();
        HtmlTagBuilder checkbox = th.append().tag().byName("input");
        checkbox.attributes().byName("type", "checkbox").className("export-all");
        //language=JS
        checkbox.attributes().onClick("BulkPdfExporter.selectAllItems(this);");

        for (Field column : this.columns) {
            row.append().tag().th().append().text(column.label());
        }
    }

    private void renderFooter(@NotNull HtmlTagBuilder tagBuilder) {
        (new BottomQueryLinksBuilder(this.context, this.dataSet, this.topItems)).render(tagBuilder);
    }

}
