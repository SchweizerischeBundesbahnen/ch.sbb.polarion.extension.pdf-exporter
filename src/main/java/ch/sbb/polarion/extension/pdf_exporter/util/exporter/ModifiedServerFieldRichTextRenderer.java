package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictHashSet;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.DleReferencesCollector;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Copy of {@link ServerFieldRichTextRenderer} with modified {@link #renderDescription(HtmlContentBuilder, WorkItemReference, boolean)}
 * to temporary replacement RichTextRenderTarget with another one to render comment icons.
 * <p>
 * Polarion renders no comment icon in the description of a work item the document references from another one: the
 * comment would belong to the document which holds the work item. A comment of this document placed there is one of its
 * own, though, and the export places it where it stands as for any other work item. Polarion still renders only the
 * comments this document has, and each of them once.
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
            return super.renderDescription(builder, workItem, withNA);
        } finally {
            FieldUtils.writeField(context, "renderTarget", backupTarget, true);
            if (external) {
                referencesCollector.addExternalWorkItem(workItem);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private static @NotNull StrictHashSet<WorkItemReference> externalWorkItemsOf(@NotNull DleReferencesCollector referencesCollector) {
        return (StrictHashSet<WorkItemReference>) FieldUtils.readField(referencesCollector, EXTERNAL_WORK_ITEMS, true);
    }
}
