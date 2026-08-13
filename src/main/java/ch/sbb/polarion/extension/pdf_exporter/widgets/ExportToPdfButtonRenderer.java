package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.alm.server.api.model.rp.widget.AbstractWidgetRenderer;
import com.polarion.alm.server.api.model.rp.widget.OpenInTableButtonWidgetRenderer;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Renders the "Export to PDF Button" report widget: a button that opens the export dialog for the report it sits on.
 * <p>
 * The dialog is a React module of the {@code pdf-exporter-app} webapp, imported on click. It mounts itself into a
 * shadow root of its own, so nothing else has to be on the page for it - unlike its predecessor, which needed the
 * micromodal library and the generic control stylesheets injected by {@code js/live-reports.js} first.
 */
public class ExportToPdfButtonRenderer extends AbstractWidgetRenderer {

    static final String POPUP_MODULE_URL = "/polarion/pdf-exporter-app/ui/app/assets/export-popup.js";

    public ExportToPdfButtonRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
    }

    @Override
    protected void render(@NotNull final HtmlFragmentBuilder builder) {
        OpenInTableButtonWidgetRenderer button = new OpenInTableButtonWidgetRenderer("Export to PDF", null, null) {
            @Override
            protected void configureLinkAttributes(@NotNull HtmlTagBuilder a) {
                a.attributes().onClick(builder.target().escapeForAttribute(onClickAction()));
            }
        };
        button.render(this.context, builder);
    }

    /**
     * The click handler: imports the dialog module and opens it for the report this button sits on.
     * <p>
     * Package-private so that it can be asserted without a rendering context. The URL and the export it calls are a
     * plain string with no compile-time link to the module it names, which is what makes them worth a test.
     */
    @VisibleForTesting
    static @NotNull String onClickAction() {
        //language=JS
        return """
                import('%s?v=%s')
                    .then(module => module.openExportPopup({documentType: 'LIVE_REPORT'}))
                    .catch(console.error);""".formatted(POPUP_MODULE_URL, getBundleVersion());
    }

    /**
     * Busts the browser cache of the dialog when the extension is updated: it is loaded from a fixed URL, as the
     * renderer cannot know the hashed file names Vite emits for the rest of the bundle.
     */
    private static @NotNull String getBundleVersion() {
        String version = VersionUtils.getVersion().getBundleVersion();
        return version == null ? "0" : version;
    }
}
