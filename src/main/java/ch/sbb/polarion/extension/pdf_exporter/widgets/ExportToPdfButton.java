package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import org.jetbrains.annotations.NotNull;

public class ExportToPdfButton extends AbstractPdfExporterButtonWidget {

    public ExportToPdfButton() {
        super("Export to PDF Button", "Renders a button which opens a popup of exporting the report to PDF");
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        renderingContext.setInlineBlockStyle();
        return (new ExportToPdfButtonRenderer(renderingContext)).render();
    }

}
