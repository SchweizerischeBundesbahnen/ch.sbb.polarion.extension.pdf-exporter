package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.TableWidget;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.FieldsParameter;
import com.polarion.alm.shared.api.model.rp.parameter.IntegerParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.SortingParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.reina.web.js.widgets.WidgetUtil;
import org.jetbrains.annotations.NotNull;

public class BulkPdfExportWidget extends TableWidget {

    @NotNull
    @Override
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return "/polarion/pdf-exporter-admin/ui/images/app-icon.svg";
    }

    @NotNull
    public Iterable<String> getTags(@NotNull SharedContext context) {
        return new ImmutableStrictList<>(Constants.PDF_EXPORT_TAG);
    }

    @NotNull
    @Override
    public String getLabel(@NotNull SharedContext sharedContext) {
        return "Bulk PDF Export";
    }

    @NotNull
    @Override
    public String getDetailsHtml(@NotNull RichPageWidgetContext richPageWidgetContext) {
        return "Renders a widget providing functionality of exporting multiple documents, reports and test runs to PDF in a single run";
    }

    @NotNull
    @Override
    public ReadOnlyStrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory parameterFactory) {
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl();
        SharedLocalization localization = parameterFactory.context().localization();
        FieldsParameter columns = parameterFactory.fields(localization.getString("richpages.widget.table.columns")).build();
        SortingParameter sortBy = parameterFactory.sorting(localization.getString("richpages.widget.table.sortBy")).build();
        DataSetParameter dataSet = parameterFactory.dataSet(localization.getString("richpages.widget.table.dataSet"))
                .allowedPrototypes(PrototypeEnum.Document, PrototypeEnum.TestRun, PrototypeEnum.RichPage, PrototypeEnum.BaselineCollection)
                .add("columns", columns)
                .add("sortBy", sortBy)
                .dependencySource(true)
                .build();
        parameters.put("dataSet", dataSet);
        parameters.put("propertiesSidebarFields", this.constructSidebarFieldsParameter(parameterFactory));
        IntegerParameter top = parameterFactory.integer(localization.getString("richpages.widget.table.top")).value(50).build();
        CompositeParameter advanced = parameterFactory.composite(localization.getString("richpages.widget.advanced")).collapsedByDefault(true).add("top", top).build();
        parameters.put("advanced", advanced);
        return parameters;
    }

    @NotNull
    private FieldsParameter constructSidebarFieldsParameter(@NotNull ParameterFactory factory) {
        FieldsParameter parameter = factory.fields(factory.context().localization().getString("richpages.widget.table.parameters.propertiesSidebarFields")).disallowedFields(WidgetUtil.getExcludedPropertiesSidebarFields()).dependencyTarget(true).build();
        parameter.defaultFields().set(WidgetUtil.getDefaultWidgetPropertiesSidebarFields());
        return parameter;
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        return (new BulkPdfExportWidgetRenderer(renderingContext)).render();
    }
}
