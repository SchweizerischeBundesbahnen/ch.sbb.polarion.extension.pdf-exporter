package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.ParameterFactoryImpl;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidget;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import org.jetbrains.annotations.NotNull;

public class BulkPdfExportWidget extends RichPageWidget {

    public static final String INCLUDE_DOCUMENTS = "includeDocuments";
    public static final String INCLUDE_REPORTS = "includeReports";
    public static final String INCLUDE_TEST_RUNS = "includeTestRuns";

    @NotNull
    @Override
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return "/polarion/ria/images/widgets/table.png";
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
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl<>();
        parameters.put(INCLUDE_DOCUMENTS, parameterFactory.bool("Include Live Documents").value(true).build());
        parameters.put(INCLUDE_REPORTS, parameterFactory.bool("Include Live Reports").value(true).build());
        parameters.put(INCLUDE_TEST_RUNS, parameterFactory.bool("Include Test Runs").value(true).build());
        return parameters;
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        return (new BulkPdfExportWidgetRenderer(renderingContext)).render();
    }
}
