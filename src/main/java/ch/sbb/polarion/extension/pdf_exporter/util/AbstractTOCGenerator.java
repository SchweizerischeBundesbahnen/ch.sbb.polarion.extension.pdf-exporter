package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractTOCGenerator implements DocumentTOCGenerator {
    protected static final int START_DEFAULT_NODE_NESTING = 1;
    protected static final int MAX_DEFAULT_NODE_NESTING = 6;

    @Override
    public @NotNull String addTableOfContent(@NotNull String html) {
        // fix HTML adding close tag for <pd4ml:toc> -- jsoup wants it
        String fixedHtml = html.replaceAll("(<pd4ml:toc[^>]*)(>)", "$1></pd4ml:toc>");

        Document doc = Jsoup.parse(fixedHtml);

        // find <pd4ml:toc> and replace
        Element tocPlaceholder = doc.getElementsByTag("pd4ml:toc").first();
        if (tocPlaceholder != null) {
            int startLevel = tocPlaceholder.hasAttr("tocInit") ? Integer.parseInt(tocPlaceholder.attr("tocInit")) : START_DEFAULT_NODE_NESTING;
            int maxLevel = tocPlaceholder.hasAttr("tocMax") ? Integer.parseInt(tocPlaceholder.attr("tocMax")) : MAX_DEFAULT_NODE_NESTING;
            String tocHtml = generateTableOfContent(doc, startLevel, maxLevel); // support h1-h6
            return html.replaceAll("(<pd4ml:toc[^>]*)(>)", tocHtml);
        }

        return html;
    }

    @NotNull
    private String generateTableOfContent(@NotNull Document document, int startLevel, int maxLevel) {
        TocLeaf root = new TocLeaf(null, 0, null, null, null);
        AtomicReference<TocLeaf> current = new AtomicReference<>(root);

        // build selector for headings (h1-h6)
        String selector = getHeadingSelector(startLevel, maxLevel);
        Elements headings = document.select(selector);

        for (Element heading : headings) {
            int level = getLevel(heading);
            String id = getId(heading);
            String number = getNumber(heading);
            String text = getText(heading);

            TocLeaf parent;
            if (current.get().getLevel() < level) {
                parent = current.get();
            } else {
                parent = current.get().getParent();
                while (parent.getLevel() >= level) {
                    parent = parent.getParent();
                }
            }

            TocLeaf newLeaf = new TocLeaf(parent, level, id, number, text);
            parent.getChildren().add(newLeaf);
            current.set(newLeaf);
        }

        return root.asString(startLevel, maxLevel);
    }

    protected int getLevel(@NotNull Element heading) {
        return Integer.parseInt(heading.tagName().substring(1)); // extract level from tag name (e.g., h1 -> 1)
    }

    protected abstract @Nullable String getId(@NotNull Element heading);

    protected abstract @Nullable String getNumber(@NotNull Element heading);

    protected abstract @NotNull String getText(@NotNull Element heading);

    protected String getHeadingSelector(int startLevel, int maxLevel) {
        StringBuilder selector = new StringBuilder();
        for (int i = startLevel; i <= maxLevel; i++) {
            if (!selector.isEmpty()) {
                selector.append(", ");
            }
            selector.append("h").append(i); // add h1, h2, ... to selector
        }
        return selector.toString();
    }

}
