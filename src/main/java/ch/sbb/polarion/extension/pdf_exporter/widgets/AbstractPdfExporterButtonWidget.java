package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.GenericButtonWidget;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for the PDF Exporter button widgets. Holds everything shared by every button widget of this extension:
 * the icon, tags, label, details HTML and an empty parameter set. Label and details are supplied through the
 * constructor so concrete widgets only declare what actually differs (parameters and rendering). Subclasses keep a
 * no-arg constructor so Polarion can instantiate them by reflection.
 */
public abstract class AbstractPdfExporterButtonWidget extends GenericButtonWidget {

    protected static final String APP_ICON = "/polarion/pdf-exporter-admin/ui/images/app-icon.svg";

    private final String label;
    private final String detailsHtml;

    protected AbstractPdfExporterButtonWidget(@NotNull String label, @NotNull String detailsHtml) {
        this.label = label;
        this.detailsHtml = detailsHtml;
    }

    @NotNull
    @Override
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return APP_ICON;
    }

    @NotNull
    @Override
    public Iterable<String> getTags(@NotNull SharedContext context) {
        return new ImmutableStrictList<>(Constants.PDF_EXPORT_TAG);
    }

    @NotNull
    @Override
    public String getLabel(@NotNull SharedContext sharedContext) {
        return label;
    }

    @NotNull
    @Override
    public String getDetailsHtml(@NotNull RichPageWidgetContext widgetContext) {
        return detailsHtml;
    }

    @NotNull
    @Override
    public ReadOnlyStrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory parameterFactory) {
        return new StrictMapImpl<>();
    }

}
