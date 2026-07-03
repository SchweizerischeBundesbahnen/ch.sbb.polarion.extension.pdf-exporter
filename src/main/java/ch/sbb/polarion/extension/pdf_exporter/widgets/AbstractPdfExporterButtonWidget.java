package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.GenericButtonWidget;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for the PDF Exporter button widgets. Holds the icon and tags shared by every button widget of this
 * extension so concrete widgets only declare what differs (label, details, parameters, rendering).
 */
public abstract class AbstractPdfExporterButtonWidget extends GenericButtonWidget {

    protected static final String APP_ICON = "/polarion/pdf-exporter-admin/ui/images/app-icon.svg";

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

}
