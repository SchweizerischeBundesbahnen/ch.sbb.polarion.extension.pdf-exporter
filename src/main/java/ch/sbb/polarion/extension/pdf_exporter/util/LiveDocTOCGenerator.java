package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class LiveDocTOCGenerator implements DocumentTOCGenerator {
    private static final String NUMBER = "number";

    @Override
    public @NotNull String addTableOfContent(@NotNull String html) {
        final int MAX_DEFAULT_NODE_NESTING = 6;

        int startIndex = html.indexOf("<pd4ml:toc");
        RegexMatcher tocInitMatcher = RegexMatcher.get("tocInit=\"(?<startLevel>\\d+)\"");
        RegexMatcher tocMaxMatcher = RegexMatcher.get("tocMax=\"(?<maxLevel>\\d+)\"");
        while (startIndex >= 0) {
            int endIndex = html.indexOf(">", startIndex);
            String tocMacro = html.substring(startIndex, endIndex);

            int startLevel = tocInitMatcher.findFirst(tocMacro, regexEngine -> regexEngine.group("startLevel"))
                    .map(Integer::parseInt).orElse(1);

            int maxLevel = tocMaxMatcher.findFirst(tocMacro, regexEngine -> regexEngine.group("maxLevel"))
                    .map(Integer::parseInt).orElse(MAX_DEFAULT_NODE_NESTING);

            String toc = generateTableOfContent(html, startLevel, maxLevel);

            html = html.substring(0, startIndex) + toc + html.substring(endIndex + 1);


            startIndex = html.indexOf("<pd4ml:toc", endIndex);
        }

        return html;
    }

    @NotNull
    @SuppressWarnings("java:S5852") //regex checked
    private String generateTableOfContent(@NotNull String html, int startLevel, int maxLevel) {
        TocLeaf root = new TocLeaf(null, 0, null, null, null);
        AtomicReference<TocLeaf> current = new AtomicReference<>(root);

        // This regexp searches for headers of any level (elements <h1>, <h2> etc.). Level of chapter is extracted into
        // named group "level", id of <a> element inside of it (to reference from TOC) - into named group "id",
        // number of this chapter - into named group "number" and text of this header - into named group "text"
        // Also we search for wiki headers, they have slightly different structure + don't have numbers
        RegexMatcher.get("<h(?<level>[1-6])[^>]*?>[^<]*(<a id=\"(?<id>[^\"]+?)\"[^>]*?></a>[^<]*<span[^>]*>\\s*<span[^>]*>(?<number>.+?)</span>[^<]*</span>\\s*(?<text>.+?)\\s*" +
                "|<span id=\"(?<wikiHeaderId>[^\"]+?)\"[^>]*?>(?<wikiHeaderText>.+?)</span>)</h[1-6]>").processEntry(html, regexEngine -> {
            // Then we take all these named groups of certain chapter and generate appropriate element of table of content
            int level = Integer.parseInt(regexEngine.group("level"));
            String id = regexEngine.group("id");
            String number = regexEngine.group(NUMBER);
            String text = regexEngine.group("text");
            String wikiHeaderId = regexEngine.group("wikiHeaderId");
            String wikiHeaderText = regexEngine.group("wikiHeaderText");

            TocLeaf parent;
            TocLeaf newLeaf;
            if (current.get().getLevel() < level) {
                parent = current.get();
            } else {
                parent = current.get().getParent();
                while (parent.getLevel() >= level) {
                    parent = parent.getParent();
                }
            }

            newLeaf = new TocLeaf(parent, level, id != null ? id : wikiHeaderId, number, text != null ? text : wikiHeaderText);
            parent.getChildren().add(newLeaf);

            current.set(newLeaf);
        });

        return root.asString(startLevel, maxLevel);
    }
}
