package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.service.PolarionBaselineExecutor;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import com.polarion.platform.persistence.model.IPObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

/**
 * Contributes the "PDF Exporter" pane to the document editor's Document Properties sidebar.
 * <p>
 * The pane itself is a React app: this extension only emits the fragment which imports it (see
 * {@code webapp/pdf-exporter/html/sidePanelContent.html}), and the panel reads everything it offers - the
 * suitable style packages, the setting names, the link roles, the default file name, the document language
 * and the export permission - from the extension's own REST API, which is where the DLE toolbar's export
 * popup has always read the same data from.
 * <p>
 * It used to render the whole form here instead, substituting ~25 placeholders into that HTML file. That is
 * gone: the panel is built once, in one language, and the server side no longer has a second copy of the
 * form's defaults to keep in step with the popup's.
 */
public class PdfExporterFormExtension implements IFormExtension {

    @VisibleForTesting
    static final String SIDE_PANEL_FRAGMENT = "webapp/pdf-exporter/html/sidePanelContent.html";

    @Override
    @Nullable
    public String render(@NotNull IFormExtensionContext context) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            String baselineRevision = transaction.context().baselineRevision();
            return PolarionBaselineExecutor.executeInBaseline(baselineRevision, transaction, () -> renderForm(transaction.context(), context.object().getOldApi()));
        });
    }

    public String renderForm(@NotNull SharedContext context, @NotNull IPObject object) {
        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilderFor().gwt();

        // Only documents can be exported by this panel, so nothing is contributed for anything else.
        if (object instanceof IModule) {
            builder.html(getSidePanelFragment());
        }

        builder.finished();
        return builder.toString();
    }

    /**
     * The fragment, with the extension version put into the bundle URL. The panel is imported from a fixed
     * URL - the fragment cannot know the hashed file names Vite emits for the rest of the bundle - so the
     * version is what busts the browser's cache of it when the extension is updated.
     */
    @VisibleForTesting
    @NotNull
    String getSidePanelFragment() {
        String version = VersionUtils.getVersion().getBundleVersion();
        return ScopeUtils.getFileContent(SIDE_PANEL_FRAGMENT).replace("{BUNDLE_VERSION}", version == null ? "0" : version);
    }

    @Override
    @Nullable
    public String getIcon(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return null;
    }

    @Override
    @Nullable
    public String getLabel(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return "PDF Exporter";
    }
}
