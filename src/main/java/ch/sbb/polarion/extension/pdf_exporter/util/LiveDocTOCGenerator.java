package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

public class LiveDocTOCGenerator extends AbstractTOCGenerator {

    @Override
    protected @Nullable String getId(@NotNull Element heading) {
        Element anchorElement = heading.selectFirst("a[id]");
        return anchorElement != null ? anchorElement.id() : null;
    }

    @Override
    protected @Nullable String getNumber(@NotNull Element heading) {
        Element spanElement = heading.selectFirst("span span");
        return spanElement != null ? spanElement.text() : null;
    }

    @Override
    protected @NotNull String getText(@NotNull Element heading) {
        return heading.ownText();
    }

}
