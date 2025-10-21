package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class JSoupUtils {
    private static final String BR_TAG  = "br";
    private static final String BUTTON_TAG  = "button";
    private static final String CANVAS_TAG  = "canvas";
    private static final String EMBED_TAG = "embed";
    private static final String IMG_TAG     = "img";
    private static final String INPUT_TAG = "input";
    private static final String HR_TAG = "hr";
    private static final String MAP_TAG = "map";
    private static final String METER_TAG = "meter";
    private static final String OBJECT_TAG = "object";
    private static final String PICTURE_TAG = "picture";
    private static final String PROGRESS_TAG = "progress";
    private static final String TABLE_TAG = "table";
    private static final String TEXTAREA_TAG = "textarea";
    private static final String SELECT_TAG = "select";
    private static final String SVG_TAG = "svg";
    private static final String VIDEO_TAG   = "video";

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
            } else if (isNonEmptyTextNode(current)) { // Check if text node has non-whitespace content
                return false;
            }

            current = current.nextSibling();
        }

        // If we reached the end of siblings without finding content, the chapter is empty
        return true;
    }

    public boolean isNonEmptyTextNode(@NotNull Node node) {
        return node instanceof TextNode textNode && !textNode.text().trim().isEmpty();
    }

    public boolean isHeading(@NotNull Node node) {
        // <h1>, <h2>, <h3> etc. - are headings, but <hr> is not
        return node instanceof Element element && element.tagName().length() == 2 && element.tagName().startsWith("h") && !element.tagName().endsWith("r");
    }

}
