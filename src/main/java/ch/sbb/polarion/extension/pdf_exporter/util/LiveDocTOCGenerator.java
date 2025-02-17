package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

public class LiveDocTOCGenerator extends AbstractTOCGenerator {

    @Override
    protected @Nullable String getId(@NotNull Element heading) {
        return heading.selectFirst("a[id]") != null ? heading.selectFirst("a[id]").id() : null;
    }

    @Override
    protected @Nullable String getNumber(@NotNull Element heading) {
        return heading.selectFirst("span span") != null ? heading.selectFirst("span span").text() : null;
    }

    @Override
    protected @NotNull String getText(@NotNull Element heading) {
        return heading.ownText();
    }

}
