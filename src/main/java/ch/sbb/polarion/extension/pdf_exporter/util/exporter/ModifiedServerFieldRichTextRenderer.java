package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another to render comment icons.
 */
public class ModifiedServerFieldRichTextRenderer extends ServerFieldRichTextRenderer {

    public ModifiedServerFieldRichTextRenderer(@NotNull ReadOnlyTransaction transaction) {
        super(transaction);
    }

    @Override
    public boolean renderDescription(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, boolean withNA) {
        RichTextRenderingContext contextBackup = this.context;
        this.context = new RichTextRenderingContext(context.getUiContext(), RichTextRenderTarget.PREVIEW);
        try {
            return super.renderDescription(builder, workItem, withNA);
        } finally {
            this.context = contextBackup;
        }
    }

}
