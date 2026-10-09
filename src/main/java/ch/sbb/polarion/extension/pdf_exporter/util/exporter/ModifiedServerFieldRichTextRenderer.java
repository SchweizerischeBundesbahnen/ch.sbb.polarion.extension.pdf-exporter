package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another one to render comment icons.
 * <p>
 * Polarion renders no comment icon in the description of a work item the document references from another one. A
 * marker there holds the id of a comment of the document which owns the work item, so this document would render its
 * own comment with that id instead. The export keeps that check.
 * </p>
 * <p>
 * A comment made in the UI on the text of a referenced work item is not stored in its description, which belongs to the
 * other document. It is a comment of this document which refers to the work item, and Polarion renders its icon after the
 * description, in {@link #renderReferredCommentMarkers}. That method renders nothing for a PDF or a print target, so
 * the export renders it as a preview too.
 * </p>
 */
public class ModifiedServerFieldRichTextRenderer extends ServerFieldRichTextRenderer {

    private static final String RENDER_TARGET = "renderTarget";

    public ModifiedServerFieldRichTextRenderer(@NotNull ReadOnlyTransaction transaction) {
        super(transaction);
    }

    @Override
    @SneakyThrows
    public boolean renderDescription(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, boolean withNA) {
        RichTextRenderTarget backupTarget = this.context.getRenderTarget();
        FieldUtils.writeField(context, RENDER_TARGET, RichTextRenderTarget.PREVIEW, true);
        try {
            return super.renderDescription(builder, workItem, withNA);
        } finally {
            FieldUtils.writeField(context, RENDER_TARGET, backupTarget, true);
        }
    }

    @Override
    @SneakyThrows
    public @NotNull ImmutableStrictList<String> renderReferredCommentMarkers(@NotNull HtmlContentBuilder builder, @NotNull DocumentReference documentReference, @NotNull WorkItemReference workItemReference) {
        RichTextRenderTarget backupTarget = this.context.getRenderTarget();
        FieldUtils.writeField(context, RENDER_TARGET, RichTextRenderTarget.PREVIEW, true);
        try {
            return super.renderReferredCommentMarkers(builder, documentReference, workItemReference);
        } finally {
            FieldUtils.writeField(context, RENDER_TARGET, backupTarget, true);
        }
    }
}
