package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictHashSet;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.DleReferencesCollector;
import com.polarion.alm.shared.rt.nodes.impl.finalnodes.RichTextCommentMarker;
import com.polarion.alm.tracker.model.IModuleComment;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another one to render comment icons.
 * <p>
 * Polarion renders no comment icon in the description of a work item the document references from another one: the
 * comment would belong to the document which holds the work item. A comment of this document placed there is one of its
 * own, though, and the export places it where it stands as for any other work item. Polarion still renders only the
 * comments this document has, and each of them once.
 * </p>
 * <p>
 * A comment added in the description of a referenced work item has no mark in the description at all: Polarion keeps the
 * work item in the comment ({@link IModuleComment#getReferredWorkItem()}) and its editor shows the icon at the end of the
 * description. The export places the comment there too, with the mark the comments are placed by.
 * </p>
 */
public class ModifiedServerFieldRichTextRenderer extends ServerFieldRichTextRenderer {

    private static final String EXTERNAL_WORK_ITEMS = "externalWorkItems";

    public ModifiedServerFieldRichTextRenderer(@NotNull ReadOnlyTransaction transaction) {
        super(transaction);
    }

    @Override
    @SneakyThrows
    public boolean renderDescription(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, boolean withNA) {
        RichTextRenderTarget backupTarget = this.context.getRenderTarget();
        DleReferencesCollector referencesCollector = this.context.dle().getReferencesCollector();
        boolean external = referencesCollector.isExternalWorkItem(workItem);
        FieldUtils.writeField(context, "renderTarget", RichTextRenderTarget.PREVIEW, true);
        if (external) {
            externalWorkItemsOf(referencesCollector).remove(workItem.getCurrent());
        }
        try {
            boolean rendered = super.renderDescription(builder, workItem, withNA);
            if (external) {
                markCommentsReferringTo(builder, workItem, referencesCollector);
            }
            return rendered;
        } finally {
            FieldUtils.writeField(context, "renderTarget", backupTarget, true);
            if (external) {
                referencesCollector.addExternalWorkItem(workItem);
            }
        }
    }

    /**
     * Marks the comments of the document which refer to the work item and are not placed yet, as Polarion's editor shows them.
     */
    private void markCommentsReferringTo(@NotNull HtmlContentBuilder builder, @NotNull WorkItemReference workItem, @NotNull DleReferencesCollector referencesCollector) {
        DocumentReference documentReference = this.context.getDocumentReference();
        IWorkItem referredWorkItem = workItemOf(workItem);
        if (documentReference == null || referredWorkItem == null || !(documentReference.get(this.transaction) instanceof InternalDocument document)) {
            return;
        }
        for (IModuleComment comment : document.getOldApi().findCommentsReferencingWorkItem(referredWorkItem)) {
            String commentId = comment.getId();
            if (!referencesCollector.getReferencedCommentsIds().contains(commentId)) {
                referencesCollector.addReferencedCommentId(commentId);
                builder.html("<span id=\"%s%s\"></span>".formatted(RichTextCommentMarker.ID_PREFIX, commentId));
            }
        }
    }

    @VisibleForTesting
    @Nullable IWorkItem workItemOf(@NotNull WorkItemReference workItem) {
        return getWorkItem(workItem);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private static @NotNull StrictHashSet<WorkItemReference> externalWorkItemsOf(@NotNull DleReferencesCollector referencesCollector) {
        return (StrictHashSet<WorkItemReference>) FieldUtils.readField(referencesCollector, EXTERNAL_WORK_ITEMS, true);
    }
}
