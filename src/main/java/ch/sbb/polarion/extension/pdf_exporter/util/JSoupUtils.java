package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.property.ECSSProperty;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

@UtilityClass
public class JSoupUtils {

    /** The tag Polarion leaves where a table of contents goes; {@link AbstractTOCGenerator} builds the table over it. */
    public static final String TOC_PLACEHOLDER_TAG = "pd4ml:toc";

    /** The element Polarion leaves where a table of figures or of tables goes; {@code HtmlProcessor.addTableOfFigures} builds it. */
    public static final String TOF_PLACEHOLDER_SELECTOR = "div[id*=macro name=tof][data-sequence]";

    /** The caption spans a table of figures or of tables is built over. Add the sequence of a table to it. */
    public static final String CAPTION_SELECTOR = "p.polarion-rte-caption-paragraph span.polarion-rte-caption";

    /** The heading levels HTML has, and the ones a table of contents lists where its placeholder names none. */
    private static final int MIN_HEADING_LEVEL = 1;
    private static final int MAX_HEADING_LEVEL = 6;

    private static final List<String> WITHOUT_TEXT_BUT_VISIBLE = List.of(
            HtmlTag.BUTTON,
            HtmlTag.CANVAS,
            HtmlTag.EMBED,
            HtmlTag.IMG,
            HtmlTag.INPUT,
            HtmlTag.HR,
            HtmlTag.MAP,
            HtmlTag.METER,
            HtmlTag.OBJECT,
            HtmlTag.PICTURE,
            HtmlTag.PROGRESS,
            HtmlTag.TABLE,
            HtmlTag.TEXTAREA,
            HtmlTag.SELECT,
            HtmlTag.SVG,
            HtmlTag.VIDEO
    );

    /** A list item shows its marker, with text or not. A list without items shows nothing. */
    private static final List<String> LIST_TAGS = List.of(HtmlTag.LI);

    /** The tags which draw a line or a background on the spaces they hold. */
    private static final List<String> TAGS_SHOWN_ON_SPACES = List.of(HtmlTag.DEL, HtmlTag.INS, HtmlTag.MARK, HtmlTag.S, HtmlTag.STRIKE, HtmlTag.U);

    private static final List<ECSSProperty> BORDER_PROPERTIES = List.of(ECSSProperty.BORDER, ECSSProperty.BORDER_TOP, ECSSProperty.BORDER_RIGHT,
            ECSSProperty.BORDER_BOTTOM, ECSSProperty.BORDER_LEFT, ECSSProperty.BORDER_STYLE);
    private static final List<ECSSProperty> BACKGROUND_PROPERTIES = List.of(ECSSProperty.BACKGROUND, ECSSProperty.BACKGROUND_COLOR);
    private static final List<ECSSProperty> HEIGHT_PROPERTIES = List.of(ECSSProperty.HEIGHT, ECSSProperty.MIN_HEIGHT, ECSSProperty.PADDING_TOP, ECSSProperty.PADDING_BOTTOM);
    private static final List<ECSSProperty> TEXT_LINE_PROPERTIES = List.of(ECSSProperty.TEXT_DECORATION, ECSSProperty.TEXT_DECORATION_LINE);
    private static final List<String> TEXT_LINES = List.of("underline", "line-through", "overline");
    /** The parts of a background value which paint nothing: no color, no image, and the repeat, attachment, position, size and box of them. */
    private static final List<String> BACKGROUND_PARTS_WITHOUT_PAINT = List.of("none", "transparent",
            "repeat", "repeat-x", "repeat-y", "no-repeat", "space", "round",
            "scroll", "fixed", "local",
            "left", "right", "top", "bottom", "center",
            "auto", "cover", "contain",
            "border-box", "padding-box", "content-box");
    private static final String BACKGROUND_PARTS_SEPARATOR = "[\\s,/]+";
    private static final String CSS_LENGTH = "[-+]?[0-9.]+[a-z%]*";

    /** Characters which show nothing and which are no spaces either: Polarion leaves zero-width spaces behind. */
    private static final String INVISIBLE_CHARACTERS = "\u200B\u200C\u200D\u2060\uFEFF";

    @NotNull
    public Document parseHtml(@NotNull String html) {
        Document document = Jsoup.parse(html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.base)
                .prettyPrint(false);
        return document;
    }

    /** Whether nothing of the HTML shows on the page. See {@link #isVisuallyEmpty(Element)}. */
    public boolean isVisuallyEmpty(@NotNull String html) {
        return isVisuallyEmpty(parseHtml(html).body());
    }

    /**
     * Whether nothing of the element shows on the page, on a screen or in print. Only what shows nothing is empty:
     * no text but spaces, line breaks, empty elements, comments and anchors. An empty table, an empty list, spaces
     * with a background or a block with a background and a height show, and a user who leaves them in a document
     * means them. The inline style alone is read: a style which comes from a class stays unseen.
     * <p>
     * pandoc-service decides the same way for a DOCX, in _holds_page_content of app/docx_post_process.py.
     * </p>
     */
    public boolean isVisuallyEmpty(@NotNull Element element) {
        return element.select("*").stream().noneMatch(JSoupUtils::showsOnThePage);
    }

    private boolean showsOnThePage(@NotNull Element element) {
        String tagName = element.tagName();
        if (WITHOUT_TEXT_BUT_VISIBLE.contains(tagName) || LIST_TAGS.contains(tagName) || element.textNodes().stream().anyMatch(JSoupUtils::hasVisibleText)) {
            return true;
        }
        CSSDeclarationList styles = CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE));
        if (hasBorder(styles)) {
            return true;
        }
        boolean hasBackground = hasBackground(styles);
        if (hasBackground && element.isBlock() && takesHeight(element)) {
            return true; // The block has a height, so its background shows
        }
        return (hasBackground || TAGS_SHOWN_ON_SPACES.contains(tagName) || hasTextLine(styles)) && holdsSpaces(element);
    }

    /** Whether a line break or the style of the block or of a block inside it gives the block a height. */
    private boolean takesHeight(@NotNull Element block) {
        return !block.select(HtmlTag.BR).isEmpty()
                || block.select("*").stream().anyMatch(inner -> inner.isBlock() && hasHeight(CssUtils.parseDeclarations(inner.attr(HtmlTagAttr.STYLE))));
    }

    private boolean hasHeight(@NotNull CSSDeclarationList styles) {
        // The first value of the shorthand is the top padding and the third, if any, the bottom one. The others are horizontal.
        String[] padding = lowerCaseValue(styles, ECSSProperty.PADDING).split("\\s+");
        return HEIGHT_PROPERTIES.stream().map(property -> lowerCaseValue(styles, property)).anyMatch(JSoupUtils::isNonZeroLength)
                || isNonZeroLength(padding[0])
                || (padding.length > 2 && isNonZeroLength(padding[2]));
    }

    /** Whether the value holds a digit other than zero: "auto", "0" and "0px" take no room. */
    private boolean isNonZeroLength(@NotNull String value) {
        return value.matches(".*[1-9].*");
    }

    private boolean hasVisibleText(@NotNull TextNode text) {
        return text.getWholeText().codePoints().anyMatch(character ->
                !Character.isWhitespace(character) && !Character.isSpaceChar(character) && INVISIBLE_CHARACTERS.indexOf(character) < 0);
    }

    private boolean holdsSpaces(@NotNull Element element) {
        return element.select("*").stream().anyMatch(inner -> inner.textNodes().stream().anyMatch(JSoupUtils::isSpaces));
    }

    /** Whether the text takes room: spaces do, the line break and the indent which lay out the HTML source do not. */
    private boolean isSpaces(@NotNull TextNode text) {
        String wholeText = text.getWholeText();
        boolean takesRoom = wholeText.codePoints().anyMatch(character -> INVISIBLE_CHARACTERS.indexOf(character) < 0);
        return takesRoom && !(text.isBlank() && wholeText.contains("\n"));
    }

    private boolean hasBorder(@NotNull CSSDeclarationList styles) {
        return BORDER_PROPERTIES.stream().map(property -> lowerCaseValue(styles, property))
                .anyMatch(value -> !value.isEmpty() && !value.contains("none") && !value.contains("hidden") && !value.matches("0[a-z]*( .*)?"));
    }

    private boolean hasBackground(@NotNull CSSDeclarationList styles) {
        return BACKGROUND_PROPERTIES.stream().map(property -> lowerCaseValue(styles, property))
                .anyMatch(value -> !value.isEmpty() && Arrays.stream(value.split(BACKGROUND_PARTS_SEPARATOR)).anyMatch(JSoupUtils::paintsBackground));
    }

    /** Whether the part of a background value is a color or an image. Anything unknown counts as one: what shows must stay. */
    private boolean paintsBackground(@NotNull String part) {
        return !BACKGROUND_PARTS_WITHOUT_PAINT.contains(part) && !part.matches(CSS_LENGTH);
    }

    private boolean hasTextLine(@NotNull CSSDeclarationList styles) {
        return TEXT_LINE_PROPERTIES.stream().map(property -> lowerCaseValue(styles, property))
                .anyMatch(value -> TEXT_LINES.stream().anyMatch(value::contains));
    }

    private @NotNull String lowerCaseValue(@NotNull CSSDeclarationList styles, @NotNull ECSSProperty property) {
        return CssUtils.getPropertyValue(styles, property).toLowerCase(Locale.ROOT);
    }

    /**
     * Checks if an element is empty content (is not visible or is a whitespace).
     */
    public boolean isEmptyElement(@NotNull Element element) {
        String tagName = element.tagName();

        // <br> elements are considered empty - in sense of whitespace
        if (HtmlTag.BR.equals(tagName)) {
            return true;
        }

        // These elements either never contain text or not always, but still can be visible
        if (WITHOUT_TEXT_BUT_VISIBLE.contains(tagName)) {
            return false;
        }

        // A table of contents, of figures or of tables is a placeholder at this point: the tables are built
        // later in the pipeline, after the empty chapters are cut. Counted as empty, such a placeholder takes
        // the chapter holding it with it - and the table with the chapter.
        if (holdsTableWithEntries(element)) {
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

    /**
     * Whether the element stands for a table this pipeline builds later, and that table will have entries.
     * <p>
     * A placeholder whose table stays empty is left to count as empty content: the chapter holding nothing but
     * an empty list is cut, which is what happened before these tables were kept at all.
     * </p>
     */
    public boolean holdsTableWithEntries(@NotNull Element element) {
        Document document = element.ownerDocument();
        if (document == null) {
            return false;
        }
        if (TOC_PLACEHOLDER_TAG.equals(element.tagName())) {
            // a table of contents lists the headings of its own levels only, so a document without one of those
            // gets an empty table
            return IntStream.rangeClosed(tocStartLevel(element), tocMaxLevel(element))
                    .anyMatch(level -> !document.select("h" + level).isEmpty());
        }
        if (element.is(TOF_PLACEHOLDER_SELECTOR)) {
            String sequence = element.dataset().get("sequence");
            return document.select(CAPTION_SELECTOR + "[data-sequence]").stream()
                    // compared rather than selected: a sequence is a label of the document and may carry anything
                    .filter(caption -> Objects.equals(sequence, caption.dataset().get("sequence")))
                    .anyMatch(caption -> readCaptionEntry(caption) != null);
        }
        return false;
    }

    /** The number and the title of a caption, as a table of figures or of tables lists them. */
    public record CaptionEntry(@NotNull String number, @NotNull String title) {
    }

    /**
     * The entry this caption gives a table of figures or of tables, or null where it gives none: Polarion writes
     * the number into the span and the title after it, and a caption missing either one is left out of the table.
     */
    public @Nullable CaptionEntry readCaptionEntry(@NotNull Element captionSpan) {
        Node numberNode = captionSpan.childNodes().stream().filter(TextNode.class::isInstance).findFirst().orElse(null);
        Node titleNode = captionSpan.nextSibling();
        if (numberNode instanceof TextNode number && titleNode instanceof TextNode title) {
            return new CaptionEntry(number.text(), title.text());
        }
        return null;
    }

    /** The first heading level the table of contents at this placeholder lists. */
    public int tocStartLevel(@NotNull Element tocPlaceholder) {
        return tocLevel(tocPlaceholder, "tocInit", MIN_HEADING_LEVEL);
    }

    /** The last heading level the table of contents at this placeholder lists. */
    public int tocMaxLevel(@NotNull Element tocPlaceholder) {
        return tocLevel(tocPlaceholder, "tocMax", MAX_HEADING_LEVEL);
    }

    /**
     * A heading level of the placeholder, held to h1-h6: the value comes from the document and may carry
     * anything, while a level outside that range names no heading and a huge one takes a walk over the
     * integers with it.
     */
    private int tocLevel(@NotNull Element tocPlaceholder, @NotNull String attribute, int defaultLevel) {
        String level = tocPlaceholder.attr(attribute).trim();
        if (level.isEmpty()) {
            return defaultLevel;
        }
        try {
            return Math.clamp(Integer.parseInt(level), MIN_HEADING_LEVEL, MAX_HEADING_LEVEL);
        } catch (NumberFormatException e) {
            return defaultLevel;
        }
    }

    public List<Element> selectEmptyHeadings(@NotNull Document document, int headingLevel) {
        return document.select("h" + headingLevel).stream()
                .filter(heading -> JSoupUtils.isEmptyHeading(heading, headingLevel))
                .toList();
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
        return node instanceof Element element && element.tagName().equals(HtmlTag.H1);
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
        Element body = table.selectFirst("> " + HtmlTag.TBODY);
        Element container = body == null ? table : body;

        return container.select("> " + HtmlTag.TR).stream()
                .filter(JSoupUtils::containsTH)
                .toList();
    }

    /**
     * Returns table rows (direct descendants of table itself or its direct tbody)
     */
    public List<Element> getBodyRows(@NotNull Element table) {
        Element body = table.selectFirst("> " + HtmlTag.TBODY);
        Element container = body == null ? table : body;

        return container.select("> " + HtmlTag.TR).stream()
                .filter(row -> !containsTH(row))
                .toList();
    }

    public boolean containsTH(@NotNull Node row) {
        return row.childNodes().stream().anyMatch(JSoupUtils::isTH);
    }

    public boolean isTH(@NotNull Node node) {
        return node instanceof Element element && element.tagName().equals(HtmlTag.TH);
    }

    public boolean isImg(@Nullable Node node) {
        return node instanceof Element element && element.tagName().equals(HtmlTag.IMG);
    }

    public Element getSingleChildByTag(@NotNull Element element, @NotNull String tagName) {
        Element child = element.firstElementChild();
        return (child != null && child.tagName().equals(tagName) && element.children().size() == 1) ? child : null;
    }
}
