package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another one to render comment icons.
 */
public class ModifiedServerFieldRichTextRenderer extends ServerFieldRichTextRenderer {

    public ModifiedServerFieldRichTextRenderer(@NotNull ReadOnlyTransaction transaction) {
        super(transaction);
    }

    @Override
    @SneakyThrows
    public boolean renderDescription(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, boolean withNA) {
        RichTextRenderTarget backupTarget = this.context.getRenderTarget();
        FieldUtils.writeField(context, "renderTarget", RichTextRenderTarget.PREVIEW, true);
        try {
            return super.renderDescription(builder, workItem, withNA);
        } finally {
            FieldUtils.writeField(context, "renderTarget", backupTarget, true);
        }
    }

}
