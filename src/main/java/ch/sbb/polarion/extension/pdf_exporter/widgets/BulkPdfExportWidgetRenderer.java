package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.BottomQueryLinksBuilder;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
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
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.ui.shared.LinearGradientColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;

public class BulkPdfExportWidgetRenderer extends AbstractWidgetRenderer {

    private static final String CSS_IMPORT_RULE = "@import";

    private final @NotNull DataSet dataSet;

    private final @NotNull IterableWithSize<ModelObject> items;
    @Getter
    private final int topItems;
    @Getter
    private final @NotNull IterableWithSize<Field> columns;
    private final @NotNull PrototypeEnum itemsPrototype;
    private final BooleanParameter exportPages;

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
        } else {
            String panelId = "bulk-%s".formatted(UUID.randomUUID().toString());
            HtmlTagBuilder wrap = builder.tag().div();
            // sbb-ui carries generic's --sbb-* design tokens (control-tokens.css) for this widget's
            // subtree without opting into the form-field scopes: the token declarations live on
            // .sbb-ui, while generic's inputs/checkboxes rules are scoped to .form-wrapper /
            // .standard-admin-page / .modal__container. Post-#535 the tokens are gone from :root, so
            // this wrapper is what makes the widget's checkbox tokens (below) resolve on a plain
            // Polarion page.
            wrap.attributes().className("polarion-PdfExporter-BulkExportWidget sbb-ui").id(panelId);

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
            wrap.append().tag().script().append().javaScript("""
                    import('/polarion/pdf-exporter/ui/js/modules/ExportBulk.js')
                        .then(module => new module.default('#%s', '%s' === 'true'))
                        .catch(console.error);""".formatted(panelId, exportPages.value()));

            wrap.append().tag().style().append().html(inlineCss("/css/micromodal.css"));
            // The widget assembles its CSS inline and does not pull generic's common.css, so the shared
            // alert styling would be missing: control-tokens.css provides the --sbb-*-icon tokens and
            // alerts.css the notification boxes + warning/error triangle icons. Without these the export
            // dialog's warnings (e.g. the PDF/A sticky-notes notice) render as unstyled plain text.
            wrap.append().tag().style().append().html(inlineCss("/css/control-tokens.css"));
            wrap.append().tag().style().append().html(inlineCss("/css/alerts.css"));
            wrap.append().tag().style().append().html(inlineCss("/webapp/pdf-exporter/css/pdf-exporter.css"));

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
        button.attributes().id("bulk-export-pdf").className("polarion-TestsExecutionButton-buttons polarion-TestsExecutionButton-buttons-defaultCursor").style(color.getStyle());

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
                    .byName("data-id", getObjectId(item))
                    .className("export-item");

            String spaceId = getSpaceId(item);
            if (spaceId != null) {
                checkbox.attributes().byName("data-space", spaceId);
            }
            String objectName = getObjectName(item);
            if (objectName != null) {
                checkbox.attributes().byName("data-name", objectName);
            }

            for (Field column : this.columns) {
                td = builder.tag().td();
                column.render(item.fields()).withLinks(true).htmlTo(td.append());
            }
        }
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

    private @NotNull String getProjectId(@NotNull ModelObject item) {
        return ((IUniqueObject) item.getOldApi()).getProjectId();
    }

    private @Nullable String getObjectName(@NotNull ModelObject item) {
        if (item.getOldApi() instanceof IBaselineCollection baselineCollection) {
            return baselineCollection.getName();
        }
        return null;
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
        checkbox.attributes().byName("type", "checkbox").id("export-all");

        for (Field column : this.columns) {
            row.append().tag().th().append().text(column.label());
        }
    }

    private void renderFooter(@NotNull HtmlTagBuilder tagBuilder) {
        (new BottomQueryLinksBuilder(this.context, this.dataSet, this.topItems)).render(tagBuilder);
    }

    /**
     * Reads a stylesheet to be embedded into an inline style tag, dropping its @import rules.
     * <p>
     * An inline style tag has no URL of its own, so a relative @import is resolved against the page URL instead of
     * against the stylesheet. The result never exists (Polarion answers with an HTML error page, which the browser
     * then refuses as a stylesheet because of its MIME type), while the very same @import works when the stylesheet
     * is linked by href. Nothing is lost by dropping it here: every stylesheet the imports refer to is embedded
     * explicitly by the caller anyway.
     */
    @NotNull
    private String inlineCss(@NotNull String path) {
        return stripCssImports(ScopeUtils.getFileContent(path));
    }

    @VisibleForTesting
    @NotNull
    static String stripCssImports(@NotNull String css) {
        return css.lines()
                .filter(line -> !startsWithImportRule(line.stripLeading()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Only rules starting a line are recognized: comments mention "@import" mid-sentence and must stay untouched.
     * The comparison ignores case, as at-rule keywords are case-insensitive in CSS.
     */
    private static boolean startsWithImportRule(@NotNull String line) {
        return line.regionMatches(true, 0, CSS_IMPORT_RULE, 0, CSS_IMPORT_RULE.length());
    }

}
