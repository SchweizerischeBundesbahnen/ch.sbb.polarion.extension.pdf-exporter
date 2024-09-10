package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import org.jetbrains.annotations.NotNull;

public class NumberedListsSanitizer {
    private static final String LIST_START_BEGINNING = "<ol";
    private static final String LIST_START_FULL = "<ol>";
    private static final String LIST_END = "</ol>";
    private static final String ITEM_START_BEGINNING = "<li";
    private static final String ITEM_END = "</li>";

    @NotNull
    public String fixNumberedLists(@NotNull String html) {
        // Polarion generates not valid HTML for multi-level numbered lists:
        //
        // <ol>
        //   <li>first item</li>
        //   <ol>
        //     <li>sub-item</li
        //   </ol>
        // </ol>
        //
        // But by HTML specification ol/ul elements can contain only li-elements as their direct children.
        // So, valid HTML will be:
        //
        // <ol>
        //   <li>first item
        //     <ol>
        //       <li>sub-item</li
        //     </ol>
        //   </li>
        // </ol>
        //
        // This method fixes the problem described above.

        StringBuilder result = new StringBuilder();
        int listStart = html.indexOf(LIST_START_BEGINNING);
        int listEnd = HtmlUtils.getEnding(html, listStart, LIST_START_BEGINNING, LIST_END);
        if (listStart >= 0 && listEnd > 0) {
            result.append(html, 0, listStart);
        } else {
            return html;
        }

        while (listStart >= 0 && listEnd > 0) {
            result.append(getValidList(html.substring(listStart, Math.min(listEnd, html.length()))));

            listStart = html.indexOf(LIST_START_BEGINNING, listEnd);
            if (listEnd < (html.length() - 1)) {
                result.append(html, listEnd, listStart < 0 ? html.length() : listStart);
            }
            listEnd = HtmlUtils.getEnding(html, listStart, LIST_START_BEGINNING, LIST_END);
        }

        return result.toString();
    }

    private String getValidList(String listHtml) {
        StringBuilder result = new StringBuilder();
        result.append(LIST_START_FULL);

        int itemStart = listHtml.indexOf(ITEM_START_BEGINNING);
        int itemEnd = listHtml.indexOf(ITEM_END);
        while (itemStart > 0 && itemEnd > 0) {
            result.append(listHtml, itemStart, itemEnd);

            itemStart = listHtml.indexOf(ITEM_START_BEGINNING, itemEnd);
            int nestedListStart = listHtml.indexOf(LIST_START_BEGINNING, itemEnd);
            if (nestedListStart > 0 && nestedListStart < itemStart) {
                int nextedListEnd = HtmlUtils.getEnding(listHtml, nestedListStart, LIST_START_BEGINNING, LIST_END);
                result.append(getValidList(listHtml.substring(nestedListStart, nextedListEnd)));
                itemStart = listHtml.indexOf(ITEM_START_BEGINNING, nextedListEnd);
            }
            result.append(ITEM_END);

            itemEnd = listHtml.indexOf(ITEM_END, itemStart);
        }

        result.append(LIST_END);
        return result.toString();
    }

    public boolean containsNestedNumberedLists(@NotNull String html) {
        int listStart = html.indexOf(LIST_START_BEGINNING);
        if (listStart >= 0) {
            int opened = 1;
            int marker = listStart + 1;

            int nextListEnd = html.indexOf(LIST_END, marker);
            int nextListStart = html.indexOf(LIST_START_BEGINNING, marker);

            while (nextListStart > 0 && nextListEnd > 0) {
                if (nextListEnd < nextListStart) {
                    opened--; // Closing tag found earlier than opening, thus decreasing counter
                    marker = nextListEnd;
                } else {
                    opened++; // Opening tag found earlier than closing, thus increasing counter (nested list)
                    marker = nextListStart;
                    if (opened > 1) {
                        return true;
                    }
                }

                marker++;
                nextListEnd = html.indexOf(LIST_END, marker);
                nextListStart = html.indexOf(LIST_START_BEGINNING, marker);
            }
        }
        return false;
    }
}
