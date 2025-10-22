package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class JSoupUtils {
    public static final String BR_TAG = "br";
    public static final String BUTTON_TAG = "button";
    public static final String CANVAS_TAG = "canvas";
    public static final String EMBED_TAG = "embed";
    public static final String IMG_TAG = "img";
    public static final String INPUT_TAG = "input";
    public static final String H1_TAG = "h1";
    public static final String HR_TAG = "hr";
    public static final String MAP_TAG = "map";
    public static final String METER_TAG = "meter";
    public static final String OBJECT_TAG = "object";
    public static final String PICTURE_TAG = "picture";
    public static final String PROGRESS_TAG = "progress";
    public static final String TABLE_TAG = "table";
    public static final String TBODY_TAG = "tbody";
    public static final String TEXTAREA_TAG = "textarea";
    public static final String TH_TAG = "th";
    public static final String THEAD_TAG = "thead";
    public static final String TR_TAG = "tr";
    public static final String SELECT_TAG = "select";
    public static final String SVG_TAG = "svg";
    public static final String VIDEO_TAG   = "video";

    private static final List<String> WITHOUT_TEXT_BUT_VISIBLE = List.of(
            BUTTON_TAG,
            CANVAS_TAG,
            EMBED_TAG,
            IMG_TAG,
            INPUT_TAG,
            HR_TAG,
            MAP_TAG,
            METER_TAG,
            OBJECT_TAG,
            PICTURE_TAG,
            PROGRESS_TAG,
            TABLE_TAG,
            TEXTAREA_TAG,
            SELECT_TAG,
            SVG_TAG,
            VIDEO_TAG
    );

    /**
     * Checks if an element is empty content (is not visible or is a whitespace).
     */
    public boolean isEmptyElement(@NotNull Element element) {
        String tagName = element.tagName();

        // <br> elements are considered empty - in sense of whitespace
        if (BR_TAG.equals(tagName)) {
            return true;
        }

        // These elements either never contain text or not always, but still can be visible
        if (WITHOUT_TEXT_BUT_VISIBLE.contains(tagName)) {
            return false;
        }

        // Any element which contains any text inside is not empty
        if (!element.text().trim().isEmpty()) {
            return false;
        }

        Elements children = element.children();
        for (Element child : children) {
            if (!isEmptyElement(child)) {
                return false;
            }
        }

        return true;
    }

    public List<Element> selectEmptyHeadings(@NotNull Document document, int headingLevel) {
        Elements headings = document.select("h" + headingLevel);

        List<Element> emptyHeadings = new LinkedList<>();
        for (Element heading : headings) {
            if (JSoupUtils.isEmptyHeading(heading, headingLevel)) {
                emptyHeadings.add(heading);
            }
        }
        return emptyHeadings;
    }

    /**
     * Checks if a heading represents an empty chapter.
     * A chapter is empty if there are only not visible or whitespace elements between itself and next heading of same/higher level or end of parent/document.
     */
    public boolean isEmptyHeading(@NotNull Element heading, int headingLevel) {
        Node current = heading.nextSibling();
        while (current != null) {
            if (current instanceof Element element) {
                // If we encounter a heading of same or higher level, the chapter is empty
                if (isHeading(element)) {
                    int lvl = element.tagName().charAt(1) - '0';
                    if (lvl > 0 && lvl <= headingLevel) {
                        return true;
                    }
                }

                // If we encounter any non-empty content, the chapter is not empty
                if (!isEmptyElement(element)) {
                    return false;
                }
            } else if (isNotEmptyTextNode(current)) { // Check if text node has non-whitespace content
                return false;
            }

            current = current.nextSibling();
        }

        // If we reached the end of siblings without finding content, the chapter is empty
        return true;
    }

    public boolean isNotEmptyTextNode(@NotNull Node node) {
        return node instanceof TextNode textNode && !textNode.text().trim().isEmpty();
    }

    public boolean isHeading(@NotNull Node node) {
        // <h1>, <h2>, <h3> etc. - are headings, but <hr> is not
        return node instanceof Element element && element.tagName().length() == 2 && element.tagName().startsWith("h") && !element.tagName().endsWith("r");
    }

    public boolean isH1(@NotNull Node node) {
        return node instanceof Element element && element.tagName().equals(H1_TAG);
    }

    public boolean containsH1(@NotNull Node node) {
        for (Node child : node.childNodes()) {
            if (isH1(child) || containsH1(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns table rows (direct descendants of table itself or its direct tbody) which contain th-tags
     */
    public List<Element> getRowsWithHeaders(@NotNull Element table) {
        List<Element> headerRows = new ArrayList<>();

        Element body = table.selectFirst("> " + TBODY_TAG);
        Element container = body == null ? table : body;

        Elements rows = container.select("> " + TR_TAG);
        for (Element row : rows) {
            if (containsTH(row)) {
                headerRows.add(row);
            }
        }

        return headerRows;
    }

    /**
     * Returns table rows (direct descendants of table itself or its direct tbody)
     */
    public List<Element> getBodyRows(@NotNull Element table) {
        List<Element> bodyRows = new ArrayList<>();

        Element body = table.selectFirst("> " + TBODY_TAG);
        Element container = body == null ? table : body;

        Elements rows = container.select("> " + TR_TAG);
        for (Element row : rows) {
            if (!containsTH(row)) {
                bodyRows.add(row);
            }
        }

        return bodyRows;
    }

    public boolean containsTH(@NotNull Node row) {
        return row.childNodes().stream().anyMatch(JSoupUtils::isTH);
    }

    public boolean isTH(@NotNull Node node) {
        return node instanceof Element element && element.tagName().equals(TH_TAG);
    }

}
