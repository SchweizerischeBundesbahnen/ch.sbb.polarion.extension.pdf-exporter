package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractTOCGenerator implements DocumentTOCGenerator {

    @Override
    public void addTableOfContent(@NotNull Document document) {
        // find <pd4ml:toc> and replace
        Element tocPlaceholder = document.getElementsByTag(JSoupUtils.TOC_PLACEHOLDER_TAG).first();
        if (tocPlaceholder != null) {
            int startLevel = JSoupUtils.tocStartLevel(tocPlaceholder);
            int maxLevel = JSoupUtils.tocMaxLevel(tocPlaceholder);
            Element tocElement = generateTableOfContent(document, startLevel, maxLevel); // support h1-h6

            tocPlaceholder.before(tocElement);
            removeLineBreakAfterTable(tocPlaceholder);
            // unwrap() instead of remove(): jsoup 1.21.2+ treats self-closing unknown elements
            // (like <pd4ml:toc/>) as opening tags, nesting subsequent content as children.
            // unwrap() removes the tag but keeps its children in place.
            tocPlaceholder.unwrap();
        }
    }

    /**
     * Drops the line break Polarion writes behind the table of contents, which prints as a blank line.
     * <p>
     * The tables of figures and of tables carry none, and the editor shows no gap after any of the three, so
     * the exported document gets none either. Reported in #1076.
     * </p>
     */
    private void removeLineBreakAfterTable(@NotNull Element tocPlaceholder) {
        // The break follows the placeholder, or sits inside it where jsoup nested what came after a self-closing tag
        Node next = tocPlaceholder.childNodeSize() > 0 ? tocPlaceholder.childNode(0) : tocPlaceholder.nextSibling();
        while (next instanceof TextNode textNode && textNode.isBlank()) {
            next = next.nextSibling();
        }
        if (next instanceof Element element && HtmlTag.BR.equals(element.tagName())) {
            element.remove();
        }
    }

    @NotNull
    private Element generateTableOfContent(@NotNull Document document, int startLevel, int maxLevel) {
        TocLeaf rootLeaf = new TocLeaf(null, 0, null, null, null);
        AtomicReference<TocLeaf> current = new AtomicReference<>(rootLeaf);

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

        return rootLeaf.asTableOfContent(startLevel, maxLevel);
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
