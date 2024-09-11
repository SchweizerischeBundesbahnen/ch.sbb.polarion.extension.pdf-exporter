package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.OpenInTableButtonWidgetRenderer;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import org.jetbrains.annotations.NotNull;

public class ExportToPdfButtonRenderer extends AbstractWidgetRenderer {
    public ExportToPdfButtonRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
    }

    @Override
    protected void render(@NotNull final HtmlFragmentBuilder builder) {
        OpenInTableButtonWidgetRenderer button = new OpenInTableButtonWidgetRenderer("Export to PDF", null, null) {
            @Override
            protected void configureLinkAttributes(@NotNull HtmlTagBuilder a) {
                a.attributes().onClick(builder.target().escapeForAttribute("PdfExporter.openPopup({ context: (new ExportContext().path === 'testrun' ? 'test_run' : 'live_report') })"));
            }
        };
        button.render(this.context, builder);
    }
}
