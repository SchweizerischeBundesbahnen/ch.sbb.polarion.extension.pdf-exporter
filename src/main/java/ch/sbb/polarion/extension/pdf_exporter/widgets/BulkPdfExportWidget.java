package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidget;
import ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidgetRenderer;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetDependenciesContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BulkPdfExportWidget extends AbstractBulkExportWidget {

    @NotNull
    @Override
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return "/polarion/pdf-exporter-app/ui/images/app-icon.svg";
    }

    @NotNull
    @Override
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

    @Override
    protected @NotNull List<PrototypeEnum> getAllowedPrototypes() {
        return List.of(PrototypeEnum.Document, PrototypeEnum.TestRun, PrototypeEnum.RichPage, PrototypeEnum.BaselineCollection);
    }

    @Override
    protected void addDataSetParameters(@NotNull DataSetParameter.Builder dataSet, @NotNull ParameterFactory parameterFactory) {
        dataSet.add("exportPages", parameterFactory.bool("Export Pages").dependencyTarget(true).value(true).build());
    }

    @Override
    protected @NotNull AbstractBulkExportWidgetRenderer createRenderer(@NotNull RichPageWidgetRenderingContext renderingContext) {
        return new BulkPdfExportWidgetRenderer(renderingContext);
    }

    @Override
    public void processParameterDependencies(@NotNull RichPageWidgetDependenciesContext context) {
        super.processParameterDependencies(context);
        DataSetParameter dataSet = context.parameter(PARAMETER_DATA_SET);
        dataSet.get("exportPages").visuals().setVisible(dataSet.prototype() == PrototypeEnum.BaselineCollection);
    }
}
