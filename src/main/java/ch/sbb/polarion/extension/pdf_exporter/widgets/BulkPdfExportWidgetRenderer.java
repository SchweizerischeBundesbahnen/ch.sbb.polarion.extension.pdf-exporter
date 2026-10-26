package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.util.BundleCacheKey;
import ch.sbb.polarion.extension.generic.widgets.bulk.AbstractBulkExportWidgetRenderer;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the Bulk PDF Export widget as a shim which the React widget app ({@code ui/src/widget}) mounts into.
 * Generic's renderer resolves and signs the data set; this one loads the app and adds what its export dialog needs.
 */
public class BulkPdfExportWidgetRenderer extends AbstractBulkExportWidgetRenderer {

    private static final String WIDGET_MODULE_URL = "/polarion/pdf-exporter-app/ui/app/assets/bulk-widget.js";
    /** The same module in the jar, which is what its cache key is a hash of. */
    private static final String WIDGET_MODULE_PATH = "webapp/pdf-exporter-app/app/assets/bulk-widget.js";

    private final BooleanParameter exportPages;

    public BulkPdfExportWidgetRenderer(@NotNull RichPageWidgetCommonContext context) {
        super(context);
        this.exportPages = dataSetParameter.get("exportPages");
    }

    @Override
    protected void renderWidgetAppLoader(@NotNull HtmlFragmentBuilder builder, @NotNull String shimId) {
        //language=JS
        builder.tag().script().append().javaScript("""
                import('%s?v=%s')
                    .then(module => module.default('#%s'))
                    .catch(console.error);""".formatted(WIDGET_MODULE_URL, BundleCacheKey.forModule(WIDGET_MODULE_PATH), shimId));
    }

    @Override
    protected @NotNull String getShimClassName() {
        return "polarion-PdfExporter-BulkExportWidget";
    }

    @Override
    protected void addShimAttributes(@NotNull HtmlAttributesBuilder attributes) {
        super.addShimAttributes(attributes);
        // Read by the app for its export dialog. Never trusted server-side: each item is converted on its own.
        attributes
                .byName("data-document-type", getItemsType(getItemsPrototype()).name())
                .byName("data-export-pages", String.valueOf(exportPages.value()));
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
}
