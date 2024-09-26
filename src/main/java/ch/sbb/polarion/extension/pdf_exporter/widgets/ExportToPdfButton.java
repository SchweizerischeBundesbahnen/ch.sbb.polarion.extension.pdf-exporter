package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.GenericButtonWidget;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidget;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import org.jetbrains.annotations.NotNull;

public class ExportToPdfButton extends GenericButtonWidget {
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
        return "Export to PDF Button";
    }

    @NotNull
    @Override
    public String getDetailsHtml(@NotNull RichPageWidgetContext widgetContext) {
        return "Renders a button which opens a popup of exporting the report to PDF";
    }

    @NotNull
    @Override
    public ReadOnlyStrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory parameterFactory) {
        return new StrictMapImpl<>();
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        renderingContext.setInlineBlockStyle();
        return (new ExportToPdfButtonRenderer(renderingContext)).render();
    }

}
