package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives the long words of table cells, a URL or an ID, places where a line may break.
 * <p>
 * The default CSS lets a cell break a word nowhere else. A long word then still fits its column, a short one is never
 * split, and WeasyPrint need not work out a width for every character of a cell, which takes it minutes for a cell of
 * long text.
 * </p>
 */
@UtilityClass
public class LongWordsAdjuster {

    /** A word longer than this may break after a slash, an underscore, a hyphen, a dot and the like. */
    static final int LONG_WORD = 20;

    /** A word longer than this, as no word of a language is, may break anywhere, every {@value #LONG_WORD} characters. */
    static final int VERY_LONG_WORD = 40;

    /** The characters a long word may break after, as a URL, a path, an address or an ID is read in parts. */
    private static final String BREAK_AFTER = "/\\_-.?&=@";

    /** The characters Unicode allows no break before, even after a space: a closing bracket, a punctuation mark, a slash. */
    private static final String NO_BREAK_BEFORE = ")]}!?,.;:/";

    private static final String WBR = "wbr";

    /** A character which joins no word: the text of the next block, or of a table inside the cell, starts anew. */
    private static final char BOUNDARY = '\n';

    public static void addBreakPoints(@NotNull Document document) {
        for (Element cell : document.select("td, th")) {
            addBreakPoints(cell);
        }
    }

    /** The text of the cell, as one string, and where in it each of its text nodes starts. */
    private record CellText(@NotNull String text, @NotNull List<TextNode> nodes, @NotNull List<Integer> starts) {
    }

    private static void addBreakPoints(@NotNull Element cell) {
        CellText cellText = collect(cell);
        List<Integer> breaks = breakPoints(cellText.text());
        if (!breaks.isEmpty()) {
            insert(cellText, breaks);
        }
    }

    /** Collects the text of a cell, leaving out the cells of a table inside it, which are a cell of their own. */
    private static @NotNull CellText collect(@NotNull Element cell) {
        StringBuilder text = new StringBuilder();
        List<TextNode> nodes = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        collect(cell, text, nodes, starts);
        return new CellText(text.toString(), nodes, starts);
    }

    private static void collect(@NotNull Node parent, @NotNull StringBuilder text, @NotNull List<TextNode> nodes, @NotNull List<Integer> starts) {
        for (Node child : parent.childNodes()) {
            if (child instanceof TextNode textNode) {
                starts.add(text.length());
                nodes.add(textNode);
                text.append(textNode.getWholeText());
            } else if (child instanceof Element element) {
                boolean boundary = element.isBlock() || element.nameIs("br") || element.nameIs("table");
                if (boundary) {
                    text.append(BOUNDARY);
                }
                if (!element.nameIs("table")) {
                    collect(element, text, nodes, starts);
                }
                if (boundary) {
                    text.append(BOUNDARY);
                }
            }
        }
    }

    /** The places, as offsets into the text, where its long words may break. */
    static @NotNull List<Integer> breakPoints(@NotNull String text) {
        List<Integer> breaks = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int start = skipSpaces(text, index);
            int end = endOfWord(text, start);
            addBreakPoints(text, start, end, breaks);
            index = end;
        }
        return breaks;
    }

    private static int skipSpaces(@NotNull String text, int index) {
        int position = index;
        while (position < text.length() && Character.isWhitespace(text.codePointAt(position))) {
            position += Character.charCount(text.codePointAt(position));
        }
        return position;
    }

    /**
     * Where the word which starts at the given place ends: at a space, unless the next word starts with a character
     * Unicode allows no break before, as in "/-123 /-123", which is one word then.
     */
    private static int endOfWord(@NotNull String text, int start) {
        int position = start;
        while (position < text.length()) {
            int codePoint = text.codePointAt(position);
            if (codePoint == BOUNDARY) {
                return position;
            }
            if (Character.isWhitespace(codePoint)) {
                int next = skipSpaces(text, position);
                if (next >= text.length() || text.charAt(next) == BOUNDARY || NO_BREAK_BEFORE.indexOf(text.codePointAt(next)) < 0) {
                    return position;
                }
                position = next;
            } else {
                position += Character.charCount(codePoint);
            }
        }
        return position;
    }

    private static void addBreakPoints(@NotNull String text, int start, int end, @NotNull List<Integer> breaks) {
        int length = text.codePointCount(start, end);
        if (length <= LONG_WORD) {
            return;
        }
        boolean veryLong = length > VERY_LONG_WORD;
        int sinceBreak = 0;
        int position = start;
        while (position < end) {
            int codePoint = text.codePointAt(position);
            position += Character.charCount(codePoint);
            sinceBreak++;
            if (position < end && (BREAK_AFTER.indexOf(codePoint) >= 0 || veryLong && sinceBreak >= LONG_WORD)) {
                breaks.add(position);
                sinceBreak = 0;
            }
        }
    }

    /** Splits the text nodes at the break points and puts a {@code <wbr>} at each. */
    private static void insert(@NotNull CellText cellText, @NotNull List<Integer> breaks) {
        int next = 0;
        for (int node = 0; node < cellText.nodes().size() && next < breaks.size(); node++) {
            next = split(cellText.nodes().get(node), cellText.starts().get(node), breaks, next);
        }
    }

    /**
     * Splits one text node at the break points which fall into it, from the given one on.
     *
     * @return the first break point after this node
     */
    private static int split(@NotNull TextNode textNode, int start, @NotNull List<Integer> breaks, int first) {
        String value = textNode.getWholeText();
        int end = start + value.length();
        List<Node> parts = new ArrayList<>();
        int from = 0;
        int next = first;
        while (next < breaks.size() && breaks.get(next) <= end) {
            int at = breaks.get(next) - start;
            if (at > from) {
                parts.add(new TextNode(value.substring(from, at)));
            }
            parts.add(new Element(WBR));
            from = at;
            next++;
        }
        if (parts.isEmpty()) {
            return next;
        }
        if (from < value.length()) {
            parts.add(new TextNode(value.substring(from)));
        }
        // One insertion for all parts: an insertion renumbers the siblings after it, so one per part takes time growing
        // with the square of their count
        Element parent = (Element) textNode.parent();
        int index = textNode.siblingIndex();
        textNode.remove();
        parent.insertChildren(index, parts);
        return next;
    }
}
