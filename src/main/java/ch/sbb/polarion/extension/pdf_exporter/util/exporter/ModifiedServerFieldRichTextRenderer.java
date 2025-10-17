package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another one to render comment icons.
 */
public class ModifiedServerFieldRichTextRenderer extends ServerFieldRichTextRenderer {

    public ModifiedServerFieldRichTextRenderer(@NotNull ReadOnlyTransaction transaction) {
        super(transaction);
    }

    @SuppressWarnings("java:S3011") // Suppress "Reflection should not be used to increase accessibility of classes, methods, or fields" warning
    @Override
    @SneakyThrows
    public boolean renderDescription(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, boolean withNA) {
        RichTextRenderTarget backupTarget = this.context.getRenderTarget();
        Field renderTargetField = RichTextRenderingContext.class.getDeclaredField("renderTarget");
        renderTargetField.setAccessible(true);
        renderTargetField.set(context, RichTextRenderTarget.PREVIEW);
        try {
            return super.renderDescription(builder, workItem, withNA);
        } finally {
            renderTargetField.set(context, backupTarget);
        }
    }

}
