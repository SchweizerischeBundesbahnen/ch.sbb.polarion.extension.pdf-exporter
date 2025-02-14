package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class LiveReportTOCGenerator implements DocumentTOCGenerator {

    @Override
    public @NotNull String addTableOfContent(@NotNull String html) {
        // fix HTML adding close tag for <pd4ml:toc> -- jsoup wants it
        String fixedHtml = html.replaceAll("(<pd4ml:toc[^>]*)(>)", "$1></pd4ml:toc>");

        Document doc = Jsoup.parse(fixedHtml);

        // find <pd4ml:toc> and replace
        Element tocPlaceholder = doc.getElementsByTag("pd4ml:toc").first();
        if (tocPlaceholder != null) {
            String tocHtml = generateTableOfContent(doc, 1, 6); // support h1-h6
            return html.replaceAll("(<pd4ml:toc[^>]*)(>)", tocHtml);
        }

        return html;
    }

    private String generateTableOfContent(Document doc, int startLevel, int maxLevel) {
        Element tocList = new Element("ul").addClass("toc");

        // store links to current <ul> for corresponding heading levels
        Map<Integer, Element> levelToList = new HashMap<>();
        levelToList.put(startLevel, tocList);

        // selector for headings
        StringBuilder selector = new StringBuilder();
        for (int i = startLevel; i <= maxLevel; i++) {
            if (!selector.isEmpty()) {
                selector.append(", ");
            }
            selector.append("h").append(i);
        }
        Elements headings = doc.select(selector.toString());

        int currentLevel = startLevel;

        for (Element heading : headings) {
            int headingLevel = Integer.parseInt(heading.tagName().substring(1)); // getting heading level
            String id = heading.id();
            String text = heading.text();
            String number = text.split(" ")[0]; // getting chapter (for example, "1.1.1")

            Element listItem = new Element("li");
            Element link = new Element("a").attr("href", "#" + id);
            link.appendChild(new Element("span").addClass("number").text(number));
            link.appendChild(new Element("span").addClass("text").text(text.replaceFirst(number, "").trim()));

            // add element for page number
            Element pageNumber = new Element("a").attr("href", "#" + id).addClass("page-number").text("  ");
            listItem.appendChild(link);
            listItem.appendChild(pageNumber);

            // Determine where to place the list item
            if (headingLevel > currentLevel) {
                Element newSubList = new Element("ul");
                levelToList.get(currentLevel).appendChild(newSubList);
                levelToList.put(headingLevel, newSubList);
            } else if (headingLevel < currentLevel) {
                // level up until the right parent is found
                while (!levelToList.containsKey(headingLevel)) {
                    levelToList.remove(currentLevel);
                    currentLevel--;
                }
            }

            // add element to appropriate <ul>
            levelToList.get(headingLevel).appendChild(listItem);
            currentLevel = headingLevel;
        }

        return tocList.outerHtml();
    }
}
