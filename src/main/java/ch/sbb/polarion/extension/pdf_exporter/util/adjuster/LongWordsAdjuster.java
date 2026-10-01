package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import com.helger.css.decl.CSSDeclarationList;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Gives the long words of table cells, a URL or an ID, places where a line may break.
 * <p>
 * The default CSS lets a cell break a word nowhere else. A long word then still fits its column, a short one is never
 * split, and WeasyPrint need not work out a width for every character of a cell, which takes it minutes for a cell of
 * long text.
 * </p>
 * <p>
 * A table in a language, as the tables of a document with a language are, hyphenates a long word of letters instead:
 * its cell gets {@code hyphens: auto}, which breaks the word at a syllable and with a hyphen.
 * </p>
 */
@UtilityClass
public class LongWordsAdjuster {

    /** A word longer than this may break after a slash, an underscore, a hyphen, a dot and the like. */
    static final int LONG_WORD = 20;

    /**
     * A word longer than this, as no word of a language is, breaks into parts of {@value #LONG_WORD} characters at most.
     * So does a long word with a digit in it, an ID, which no word of a language is either.
     */
    static final int VERY_LONG_WORD = 40;

    /** The characters a long word may break after, as a URL, a path, an address or an ID is read in parts. */
    private static final String BREAK_AFTER = "/\\_-.?&=@";

    /** The characters Unicode allows no break before, even after a space: a closing bracket, a punctuation mark, a slash. */
    private static final String NO_BREAK_BEFORE = ")]}!?,.;:/";

    private static final String WBR = "wbr";

    private static final String TABLE = "table";

    private static final String CELL = "td, th";

    private static final String LANG = "lang";

    private static final String IN_A_LANGUAGE = "[lang]";

    /** Marks an element a rule of the CSS of the export turns hyphenation off for. */
    private static final String NO_HYPHENATION = "data-pdf-exporter-no-hyphenation";

    private static final String TURNED_OFF = "[" + NO_HYPHENATION + "]";

    /** The languages WeasyPrint hyphenates, by the dictionaries of pyphen it ships with, as primary language subtags. */
    private static final Set<String> HYPHENATION_DICTIONARIES = Set.of(
            "af", "as", "be", "bg", "ca", "cs", "da", "de", "el", "en", "eo", "es", "et", "eu", "fr", "gl", "hr", "hu", "id", "is",
            "it", "kn", "lt", "lv", "mn", "mr", "nb", "nl", "nn", "or", "pa", "pl", "pt", "ro", "ru", "sa", "sk", "sl", "sq", "sr",
            "sv", "te", "th", "uk", "zu");

    /** A character which joins no word: the text of the next block, or of a table inside the cell, starts anew. */
    private static final char BOUNDARY = '\n';

    /**
     * How long the parts of a word may be in a table its words leave no room for, tried one after the other: the parts
     * get shorter until the table fits.
     */
    public static final List<Integer> CRAMPED_PARTS = List.of(LONG_WORD, 15, 10);

    /** Where the words of the cells of a table may break: their length, from which on, and how long a part may be. */
    private record Rule(int separatorsFrom, int partsFrom, int part) {
    }

    private static final Rule DEFAULT_RULE = new Rule(LONG_WORD, VERY_LONG_WORD, LONG_WORD);

    public static void addBreakPoints(@NotNull Document document) {
        addBreakPoints(document, Hyphenation.NONE);
    }

    public static void addBreakPoints(@NotNull Document document, @Nullable String language) {
        addBreakPoints(document, new Hyphenation(language, List.of()));
    }

    /**
     * @param hyphenation the language of the document, which its tables are then marked to be in, unless they are in
     *                    one of their own, so that they hyphenate their long words of letters wherever they are laid
     *                    out; and the rules of the CSS which turn hyphenation off, whose elements are marked so
     */
    public static void addBreakPoints(@NotNull Document document, @NotNull Hyphenation hyphenation) {
        if (hyphenation.language() != null) {
            for (Element table : document.select(TABLE)) {
                if (table.closest(IN_A_LANGUAGE) == null) {
                    table.attr(LANG, hyphenation.language());
                }
            }
            markHyphenationTurnedOff(document, hyphenation);
        }
        for (Element cell : document.select(CELL)) {
            breakWordsOf(cell, DEFAULT_RULE);
        }
    }

    /**
     * Gives the words of a table more places to break, as its words leave it wider than the room it has: a word longer
     * than the given length breaks after its separators, and into parts of that length at most, as even as they can
     * be. A table which allowed a break at any character used to break its words wherever a line ended.
     */
    public static void addBreakPointsToFit(@NotNull Element table, int part) {
        Rule rule = new Rule(part, part, part);
        for (Element cell : table.select(CELL)) {
            breakWordsOf(cell, rule);
        }
    }

    /** The text of the cell, as one string, and where in it each of its text nodes starts. */
    private record CellText(@NotNull String text, @NotNull List<TextNode> nodes, @NotNull List<Integer> starts) {
    }

    /** Where the long words of a text may break, and whether a word was left to hyphenation instead. */
    private record Breaks(@NotNull List<Integer> points, boolean hyphenates) {
    }

    private static void breakWordsOf(@NotNull Element cell, @NotNull Rule rule) {
        CellText cellText = collect(cell);
        Breaks breaks = breakPoints(cellText.text(), rule, hyphenates(cell));
        if (!breaks.points().isEmpty()) {
            insert(cellText, breaks.points());
        }
        if (breaks.hyphenates()) {
            hyphenate(cell);
        }
    }

    /**
     * Marks the elements a rule of the CSS turns hyphenation off for. The rules are matched as the PDF lays the document
     * out: inside the {@code div.content} its template wraps it in, after the header and the footer. A rule for the whole
     * document, for its body or for that wrapper, as one which cannot be read, marks every table, as those elements are
     * not part of what is processed here.
     */
    private static void markHyphenationTurnedOff(@NotNull Document document, @NotNull Hyphenation hyphenation) {
        if (hyphenation.turnedOffBy().isEmpty()) {
            return;
        }
        Element body = document.body();
        Element content = new Element("div").addClass("content");
        content.insertChildren(0, new ArrayList<>(body.childNodes()));
        List<Node> pageBefore = new ArrayList<>(Jsoup.parseBodyFragment(hyphenation.pageBefore()).body().childNodes());
        body.insertChildren(0, pageBefore);
        body.appendChild(content);
        try {
            for (String selector : hyphenation.turnedOffBy()) {
                markHyphenationTurnedOff(document, selector, content);
            }
        } finally {
            pageBefore.forEach(Node::remove);
            content.unwrap();
        }
    }

    private static void markHyphenationTurnedOff(@NotNull Document document, @NotNull String selector, @NotNull Element content) {
        Elements selected;
        try {
            selected = document.select(selector);
        } catch (Selector.SelectorParseException e) {
            document.select(TABLE).attr(NO_HYPHENATION, "");
            return;
        }
        for (Element element : selected) {
            if (element == content || element == document.body() || element.nameIs("html")) {
                document.select(TABLE).attr(NO_HYPHENATION, "");
                return;
            }
            element.attr(NO_HYPHENATION, "");
        }
    }

    /**
     * Whether a cell can hyphenate its words: it is in a language WeasyPrint has a dictionary for, no rule of the CSS
     * turns hyphenation off for it, an element around it or one inside it, and neither it nor an element around it
     * states another hyphenation than {@code auto}. Where it cannot, a word keeps its break points.
     */
    private static boolean hyphenates(@NotNull Element cell) {
        Element inALanguage = cell.closest(IN_A_LANGUAGE);
        if (inALanguage == null || !HYPHENATION_DICTIONARIES.contains(inALanguage.attr(LANG).split("[-_]")[0].toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (cell.closest(TURNED_OFF) != null || cell.selectFirst(TURNED_OFF) != null) {
            return false;
        }
        for (Element element = cell; element != null; element = element.parent()) {
            String hyphens = CssUtils.getPropertyValue(CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE)), CssProp.HYPHENS);
            if (!hyphens.isEmpty()) {
                return CssProp.HYPHENS_AUTO_VALUE.equals(hyphens);
            }
        }
        return true;
    }

    /** Lets a cell break its words at a syllable, unless the document states how it hyphenates. */
    private static void hyphenate(@NotNull Element cell) {
        CSSDeclarationList style = CssUtils.parseDeclarations(cell.attr(HtmlTagAttr.STYLE));
        if (CssUtils.getPropertyValue(style, CssProp.HYPHENS).isEmpty()) {
            CssUtils.setPropertyValue(style, CssProp.HYPHENS, CssProp.HYPHENS_AUTO_VALUE);
            cell.attr(HtmlTagAttr.STYLE, style.getAsCSSString());
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
                // A place to break, given before, ends a word as a space does
                boolean boundary = element.isBlock() || element.nameIs("br") || element.nameIs(WBR) || element.nameIs(TABLE);
                if (boundary) {
                    text.append(BOUNDARY);
                }
                if (!element.nameIs(TABLE)) {
                    collect(element, text, nodes, starts);
                }
                if (boundary) {
                    text.append(BOUNDARY);
                }
            }
        }
    }

    /** The places, as offsets into the text, where its long words may break. */
    private static @NotNull Breaks breakPoints(@NotNull String text, @NotNull Rule rule, boolean canHyphenate) {
        List<Integer> breaks = new ArrayList<>();
        boolean hyphenates = false;
        int index = 0;
        while (index < text.length()) {
            int start = skipSpaces(text, index);
            int end = endOfWord(text, start);
            if (canHyphenate && text.codePointCount(start, end) > rule.partsFrom() && isAWordOfALanguage(text, start, end)) {
                hyphenates = true;
            } else {
                addWordBreakPoints(text, start, end, rule, breaks);
            }
            index = end;
        }
        return new Breaks(breaks, hyphenates);
    }

    /**
     * A word of letters alone, which a dictionary of the language can hyphenate, unlike an ID, a path or a name of code.
     * The punctuation around it, as a full stop or a closing bracket, makes it no less a word, and words a no-break space
     * joins, as in "der&nbsp;Rechtsschutzversicherungsgesellschaft", are words each.
     */
    private static boolean isAWordOfALanguage(@NotNull String text, int start, int end) {
        int[] codePoints = text.substring(start, end).codePoints().toArray();
        int wordStart = 0;
        for (int index = 0; index <= codePoints.length; index++) {
            if (index == codePoints.length || Character.getType(codePoints[index]) == Character.SPACE_SEPARATOR) {
                if (!isAWord(codePoints, wordStart, index)) {
                    return false;
                }
                wordStart = index + 1;
            }
        }
        return true;
    }

    private static boolean isAWord(int @NotNull [] codePoints, int start, int end) {
        int first = start;
        int last = end;
        while (first < last && isPunctuation(codePoints[first])) {
            first++;
        }
        while (last > first && isPunctuation(codePoints[last - 1])) {
            last--;
        }
        return first < last
                && Arrays.stream(codePoints, first, last).allMatch(codePoint -> Character.isLetter(codePoint) || Character.getType(codePoint) == Character.NON_SPACING_MARK)
                && !isCamelCase(codePoints, first, last);
    }

    /** A capital after a small letter, as in a class name, makes a name of code, which a hyphen would change. */
    private static boolean isCamelCase(int @NotNull [] codePoints, int start, int end) {
        for (int index = start + 1; index < end; index++) {
            if (Character.isUpperCase(codePoints[index]) && Character.isLowerCase(codePoints[index - 1])) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPunctuation(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.OTHER_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                 Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION -> true;
            default -> false;
        };
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

    private static void addWordBreakPoints(@NotNull String text, int start, int end, @NotNull Rule rule, @NotNull List<Integer> breaks) {
        int length = text.codePointCount(start, end);
        if (length <= rule.separatorsFrom()) {
            return;
        }
        boolean intoParts = length > rule.partsFrom() || hasDigit(text, start, end);
        int partStart = start;
        int position = start;
        while (position < end) {
            int codePoint = text.codePointAt(position);
            position += Character.charCount(codePoint);
            if (BREAK_AFTER.indexOf(codePoint) >= 0 && position < end) {
                if (intoParts) {
                    addPartBreakPoints(text, partStart, position, rule.part(), breaks);
                }
                breaks.add(position);
                partStart = position;
            }
        }
        if (intoParts) {
            addPartBreakPoints(text, partStart, end, rule.part(), breaks);
        }
    }

    private static boolean hasDigit(@NotNull String text, int start, int end) {
        return text.substring(start, end).codePoints().anyMatch(Character::isDigit);
    }

    /** Breaks a stretch of a word with no separator into parts of the given length at most, as even as they can be. */
    private static void addPartBreakPoints(@NotNull String text, int start, int end, int part, @NotNull List<Integer> breaks) {
        int length = text.codePointCount(start, end);
        if (length <= part) {
            return;
        }
        int parts = (length + part - 1) / part;
        int position = start;
        for (int index = 1; index < parts; index++) {
            int partLength = length * index / parts - length * (index - 1) / parts;
            position = text.offsetByCodePoints(position, partLength);
            breaks.add(position);
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
        Element parent = textNode.parent();
        if (parent != null) {
            int index = textNode.siblingIndex();
            textNode.remove();
            parent.insertChildren(index, parts);
        }
        return next;
    }
}
