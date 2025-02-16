package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.atomic.AtomicReference;

public class LiveDocTOCGenerator implements DocumentTOCGenerator {
    private static final int MAX_DEFAULT_NODE_NESTING = 6;

    @Override
    public @NotNull String addTableOfContent(@NotNull String html) {
        // fix HTML adding close tag for <pd4ml:toc> -- jsoup wants it
        String fixedHtml = html.replaceAll("(<pd4ml:toc[^>]*)(>)", "$1></pd4ml:toc>");

        Document doc = Jsoup.parse(fixedHtml);

        // find <pd4ml:toc> and replace
        Element tocPlaceholder = doc.getElementsByTag("pd4ml:toc").first();
        if (tocPlaceholder != null) {
            int startLevel = tocPlaceholder.hasAttr("tocInit") ? Integer.parseInt(tocPlaceholder.attr("tocInit")) : 1;
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

        // Find all headers (<h1> to <h6>) in the document
        Elements headers = document.select("h1, h2, h3, h4, h5, h6");
        for (Element header : headers) {
            int level = Integer.parseInt(header.tagName().substring(1)); // Extract level from tag name (e.g., h1 -> 1)
            String id = header.selectFirst("a[id]") != null ? header.selectFirst("a[id]").id() : null;
            String number = header.selectFirst("span span") != null ? header.selectFirst("span span").text() : null;
            String text = header.ownText();

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

}
