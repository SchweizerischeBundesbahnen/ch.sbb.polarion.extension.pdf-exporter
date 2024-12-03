package ch.sbb.polarion.extension.pdf_exporter.widgets;

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
import com.polarion.alm.shared.api.model.rp.parameter.impl.dataset.FieldImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.ui.shared.LinearGradientColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionFieldsEnum.elements;

public class BulkPdfExportWidgetRenderer extends AbstractWidgetRenderer {
    @NotNull
    private final DataSet dataSet;
    @NotNull
    private final IterableWithSize<ModelObject> items;
    private final int topItems;
    @NotNull
    private final IterableWithSize<Field> columns;
    private final String dataType;

    public BulkPdfExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
        DataSetParameter dataSetParameter = context.parameter("dataSet");
        FieldsParameter columnsParameter = dataSetParameter.get("columns");

        if (PrototypeEnum.BaselineCollection.equals(dataSetParameter.prototype())) {
            List<Field> fields = columnsParameter.fields().toArrayList();
            if (fields.stream().noneMatch(field -> "elements".equals(field.id()))) {
                fields.add(new FieldImpl("elements"));
                columnsParameter.set().fields(fields.stream().map(Field::id).collect(Collectors.toList()));
            }
        }

        SortingParameter sortByParameter = dataSetParameter.get("sortBy");
        String sort = sortByParameter.asLuceneSortString();
        this.dataSet = dataSetParameter.getFor().sort(sort).revision(null);
        this.items = this.dataSet.items();
        switch (dataSetParameter.prototype()) {
            case Document:
                dataType = "Documents";
                break;
            case RichPage:
                dataType = "Pages";
                break;
            case TestRun:
                dataType = "Test Runs";
                break;
            case BaselineCollection:
                dataType = "Collections";
                break;
            default:
                dataType = "Unknown";
                break;
        }
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

    protected void render(@NotNull HtmlFragmentBuilder builder) {
        if (this.topItems < 0) {
            builder.html(this.context.renderWarning(this.localization.getString("richpages.widget.table.invalidTopValue")));
        } else {
            HtmlTagBuilder wrap = builder.tag().div();
            wrap.attributes().className("polarion-PdfExporter-BulkExportWidget");

            HtmlTagBuilder header = wrap.append().tag().div();
            header.attributes().className("header");

            HtmlTagBuilder title = header.append().tag().h3();
            title.append().text(dataType);

            renderExportButton(header);

            HtmlTagBuilder description = header.append().tag().div();
            description.append().tag().p().append().text(String.format("Please select %s below which you want to export and click button above", dataType));

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

    private void renderItem(@NotNull HtmlContentBuilder builder, @NotNull ModelObject item) {
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
                    .byName("data-space", getSpace(item))
                    .byName("data-id", getValue(item, "id"))
                    .byName("data-name", getValue(item, "name"))
                    .className("export-item");

            for (Field column : this.columns) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
    }

    private String getSpace(@NotNull ModelObject item) {
        String spaceFieldId = null;
        if (item.getOldApi() instanceof IModule) {
            spaceFieldId = "moduleFolder";
        } else if (item.getOldApi() instanceof IRichPage) {
            spaceFieldId = "spaceId";
        }
        if (spaceFieldId != null) {
            return getValue(item, spaceFieldId);
        } else {
            return "";
        }
    }

    private String getValue(@NotNull ModelObject item, @NotNull String fieldName) {
        Object fieldValue = item.getOldApi().getValue(fieldName);
        return fieldValue == null ? "" : fieldValue.toString();
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
