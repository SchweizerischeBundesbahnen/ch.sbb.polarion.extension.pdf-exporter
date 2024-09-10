package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.dle.parts.DlePart;
import com.polarion.alm.shared.html.HtmlNode;
import com.polarion.alm.shared.rt.document.ServerRichTextDocumentFullyLoaded;
import com.polarion.alm.shared.rt.parts.impl.readonly.PageBreakPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extended version of {@link ServerRichTextDocumentFullyLoaded}.
 * Used to implement 'page break' feature using polarion native markers.
 */
public class ModifiedServerRichTextDocumentFullyLoaded extends ServerRichTextDocumentFullyLoaded {

    public ModifiedServerRichTextDocumentFullyLoaded(@NotNull InternalDocument document, @NotNull Iterable<HtmlNode> nodes, @Nullable String query, boolean fixLevel) {
        super(document, nodes, query, fixLevel);
    }

    @Override
    protected void addPart(@NotNull DlePart part) {
        if (part instanceof PageBreakPart pageBreakPart) {
            part = new CustomPageBreakPart(pageBreakPart);
        }
        super.addPart(part);
    }
}
