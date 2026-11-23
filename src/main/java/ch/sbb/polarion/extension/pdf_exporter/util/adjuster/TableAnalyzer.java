package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.property.ECSSProperty;
import com.helger.css.propertyvalue.CCSSValue;
import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.w3c.dom.Document;
import org.xhtmlrenderer.context.AWTFontResolver;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.layout.breaker.BreakPoint;
import org.xhtmlrenderer.layout.breaker.BreakPointsProvider;
import org.xhtmlrenderer.layout.breaker.LineBreakingStrategy;
import org.xhtmlrenderer.layout.breaker.UrlAwareLineBreakIterator;
import org.xhtmlrenderer.newtable.TableSectionBox;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.swing.EmptyReplacedElement;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.InstantImageReplacedElement;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.ImageUtil;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TableAnalyzer {
    private static final Logger logger = Logger.getLogger(TableAnalyzer.class);

    private static final String TABLE = "table";
    private static final String TBODY = "tbody";
    private static final String TR = "tr";
    private static final String TD = "td";
    private static final String TH = "th";
    private static final String ZERO_WIDTH_SPACE = "\u200B";
    private static final String IMG = "img";
    private static final String LANG = "lang";
    /** The language which marks a text the measure must not break, see {@link #measureNoWrapTextAsOneWord(Element)}. */
    static final String NO_BREAK_LANGUAGE = "x-pdf-exporter-nowrap";
    private static final String NO_BREAK_SPACE = "\u00A0";
    /** A length relative to something the measure knows only as it lays the table out, as the font. */
    private static final int RELATIVE = -1;
    /** The values of {@code white-space} by which an element takes it from the element around. */
    private static final Set<String> INHERITED = Set.of("inherit", "unset");
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t\\n\\r\\f]+");
    private static final Pattern LEADING_WHITESPACE = Pattern.compile("^[ \\t\\n\\r\\f]+");
    private static final Pattern CSS_LENGTH = Pattern.compile("(\\d+(?:\\.\\d+)?|\\.\\d+)(px|pt|pc|in|cm|mm|q)", Pattern.CASE_INSENSITIVE);
    /** The pixels of the absolute units of CSS (CSS Values 3, 6.2). */
    private static final Map<String, Float> PIXELS_PER_UNIT = Map.of("px", 1f, "pt", 96f / 72, "pc", 16f, "in", 96f, "cm", 96f / 2.54f, "mm", 9.6f / 2.54f, "q", 2.4f / 2.54f);

    /** The text styles which decide how tall a line of a cell is, and which a table inherits from around it. */
    private static final List<ECSSProperty> INHERITED_TEXT_PROPERTIES = List.of(ECSSProperty.FONT_SIZE, ECSSProperty.LINE_HEIGHT, ECSSProperty.FONT_WEIGHT, ECSSProperty.LETTER_SPACING);

    // Doesn't really matter, our concern here are widths
    private static final int PAGE_HEIGHT = 1000;
    /**
     * The font an export is laid out in: the default CSS asks for Arial, and the WeasyPrint service lays it out in
     * Liberation Sans, which has the metrics of Arial. Measured in it, a table takes the widths it takes in the PDF.
     */
    private static final String EMBEDDED_FONT_PATH = "/fonts/LiberationSans-Regular.ttf";
    private static final String EMBEDDED_BOLD_FONT_PATH = "/fonts/LiberationSans-Bold.ttf";
    private static final String EMBEDDED_ITALIC_FONT_PATH = "/fonts/LiberationSans-Italic.ttf";
    private static final String EMBEDDED_BOLD_ITALIC_FONT_PATH = "/fonts/LiberationSans-BoldItalic.ttf";

    /** The pixels CSS counts to an inch, which turns a font size in points into the pixels WeasyPrint lays it out in. */
    private static final float CSS_DPI = 96f;

    /**
     * The name the measurement gives the font it ships with. A name of its own, because a machine which has a
     * font of the same name installed lends its own file to the layout: its bold face is not the one shipped
     * here, the text then takes a line more or less, and the same document comes out laid out differently.
     */
    private static final String MEASUREMENT_FONT_FAMILY = "PdfExporterTableMeasurement";
    private static final Font EMBEDDED_FONT = loadEmbeddedFont();
    private static final Font EMBEDDED_BOLD_FONT = loadFontFromPath(EMBEDDED_BOLD_FONT_PATH);
    private static final Font EMBEDDED_ITALIC_FONT = loadFontFromPath(EMBEDDED_ITALIC_FONT_PATH);
    private static final Font EMBEDDED_BOLD_ITALIC_FONT = loadFontFromPath(EMBEDDED_BOLD_ITALIC_FONT_PATH);

    private static Font loadEmbeddedFont() {
        return loadFontFromPath(EMBEDDED_FONT_PATH);
    }

    static Font loadFontFromPath(String fontPath) {
        try (InputStream fontStream = TableAnalyzer.class.getResourceAsStream(fontPath)) {
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(Font.PLAIN, 12f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                logger.info("Loaded embedded font: " + font.getFontName());
                return font;
            }
        } catch (Exception e) {
            logger.warn("Failed to load embedded font from " + fontPath + ": " + e);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    /**
     * The width of every column of the table, the height its header takes and the height of each of its rows.
     *
     * @param columnWidths the width of each column, proportionally adjusted to the width of a page
     * @param headerHeight the height of the rows the table repeats on every page it spans
     * @param rowHeights   the height of each row of the table itself, not of the tables nested in it, in document order
     * @param tableWidth   the width the table takes, wider than the page where its words cannot break to fit it
     */
    public record TableMetrics(@NotNull Map<Integer, Integer> columnWidths, int headerHeight, @NotNull List<Integer> rowHeights, int tableWidth) {

        public TableMetrics(@NotNull Map<Integer, Integer> columnWidths, int headerHeight, @NotNull List<Integer> rowHeights) {
            this(columnWidths, headerHeight, rowHeights, 0);
        }
    }

    public Map<Integer, Integer> getColumnWidths(@NotNull Element tableElement, int pageWidth) {
        return analyze(tableElement, pageWidth).columnWidths();
    }

    public TableMetrics analyze(@NotNull Element tableElement, int pageWidth) {
        return analyze(tableElement, pageWidth, "");
    }

    /**
     * Measures the table as {@link #analyze(Element, int)} does, with the given CSS applied as well: rules of the export
     * which the inline styles of the table do not carry.
     */
    public TableMetrics analyze(@NotNull Element tableElement, int pageWidth, @NotNull String css) {
        Map<Integer, Integer> columnWidths = new HashMap<>();
        Gathered gathered = new Gathered();
        List<Integer> rowHeights = new ArrayList<>();

        Document doc = toSelfDocument(tableElement, css);
        Box rootBox = render(doc, pageWidth);
        findTableAndAnalyze(rootBox, columnWidths, gathered, rowHeights);

        return new TableMetrics(adjustWidths(columnWidths, pageWidth), gathered.headerHeight, rowHeights, gathered.tableWidth);
    }

    /** What a walk over the rendered table gathers: the height its header rows take, and the width of the table. */
    private static class Gathered {
        private int headerHeight;
        private int tableWidth;
    }

    private Document toSelfDocument(@NotNull Element tableElement, @NotNull String css) {
        org.jsoup.nodes.Document tempDoc = org.jsoup.nodes.Document.createShell("");
        if (!css.isBlank()) {
            tempDoc.head().appendElement("style").text(css);
        }
        // Inject CSS to force the embedded font for consistent column width calculation across platforms
        tempDoc.head().appendElement("style").text("* { font-family: '" + MEASUREMENT_FONT_FAMILY + "', sans-serif !important; }");
        Element table = tableElement.clone();
        measureBreakableWords(table);
        measureWordBreakOpportunitiesAsText(table);
        measureNoWrapTextAsOneWord(table);
        tempDoc.body().appendElement("div").attr(HtmlTagAttr.STYLE, inheritedTextStyle(tableElement)).appendChild(table);
        Document document = new W3CDom().fromJsoup(tempDoc);
        // Joins each zero width space to the text around it: the measure lays out each text node as a box of its own
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * Puts a zero width space in place of each {@code <wbr>}. The measure breaks a line at both alike, but lays out a
     * {@code <wbr>} as a box of its own, which takes time in a table of many long words.
     */
    private static void measureWordBreakOpportunitiesAsText(@NotNull Element table) {
        for (Element wbr : table.select("wbr")) {
            wbr.replaceWith(new TextNode(ZERO_WIDTH_SPACE));
        }
    }

    /**
     * Lets the measure lay out a text which may not wrap as one word. flying-saucer finds the narrowest width of each of
     * its inline boxes alone, so {@code <span style="white-space: nowrap">EL <b>17</b></span>} or the icon and the ID of
     * a linked work item measure as narrow as their widest part. A word across inline boxes it measures whole since 10.8.0,
     * so the text is turned into one: it wraps as normal text here, but its language makes
     * {@link MeasureLineBreakingStrategy} give it no place to break. Its spaces become no-break spaces, so that a space
     * before it, of the text around, stays a place to break. An image breaks a word in any case, so it gives way to an
     * empty element as wide, whose padding goes with the word. An element in it which wraps its text again, as Polarion
     * wraps the title of a linked work item, is left as it is.
     * <p>
     * Only the styles of the elements themselves are read, which is where Polarion states {@code white-space} and the size
     * of an icon: a text which a stylesheet keeps from wrapping is measured as flying-saucer measures it. To be dropped
     * once flying-saucer measures such a text whole itself (flyingsaucerproject/flyingsaucer#795).
     * </p>
     */
    private static void measureNoWrapTextAsOneWord(@NotNull Element table) {
        for (Element noWrap : table.select("[style*=nowrap]")) {
            if (!NO_BREAK_LANGUAGE.equals(noWrap.attr(LANG)) && CssProp.WHITE_SPACE_NOWRAP_VALUE.equals(whiteSpace(noWrap))) {
                new OneWord(wordWrap(noWrap)).mark(noWrap).end();
            }
        }
    }

    /**
     * Marks a text as one word, but for the elements in it which wrap their text again. Its spaces collapse as CSS
     * collapses them, across the boundaries of its elements, before they become no-break spaces, which do not collapse.
     * A space at its start or end stays a regular one, which the measure drops at the edge of a line. A word which may
     * break anywhere would break there all the same, as the measure lets it break where it wraps, so it may not here, and
     * an element which wraps its text again gets back the {@code word-wrap} it inherits from around the text.
     */
    private static final class OneWord {
        /** The {@code word-wrap} the text so far inherits, from around it or from an element in it. */
        private @NotNull String wordWrap;
        /** Whether the text so far ends in a space. */
        private boolean afterSpace;
        /** Whether the word has started: a space before it is at its start. */
        private boolean started;
        /** The text node whose space ends the word so far, if one does. */
        private @Nullable TextNode endSpace;

        private OneWord(@NotNull String wordWrap) {
            this.wordWrap = wordWrap;
        }

        private @NotNull OneWord mark(@NotNull Element element) {
            CSSDeclarationList style = CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE));
            if (CssProp.WHITE_SPACE_NOWRAP_VALUE.equals(CssUtils.getPropertyValue(style, ECSSProperty.WHITE_SPACE))) {
                CssUtils.setPropertyValue(style, ECSSProperty.WHITE_SPACE, CssProp.WHITE_SPACE_NORMAL_VALUE);
            }
            String around = wordWrap;
            String own = CssUtils.getPropertyValue(style, ECSSProperty.WORD_WRAP);
            if (!own.isEmpty()) {
                wordWrap = CssProp.WORD_WRAP_NORMAL_VALUE.equals(own) ? "" : own;
            }
            if (!wordWrap.isEmpty() || !own.isEmpty()) {
                CssUtils.setPropertyValue(style, ECSSProperty.WORD_WRAP, CssProp.WORD_WRAP_NORMAL_VALUE);
            }
            if (!style.getAllDeclarations().isEmpty()) {
                element.attr(HtmlTagAttr.STYLE, style.getAsCSSString());
            }
            element.attr(LANG, NO_BREAK_LANGUAGE);
            for (Node node : new ArrayList<>(element.childNodes())) {
                if (node instanceof TextNode text) {
                    text(text);
                } else if (node instanceof Element child) {
                    child(child);
                }
            }
            wordWrap = around;
            return this;
        }

        private void text(@NotNull TextNode text) {
            String collapsed = WHITESPACE.matcher(text.getWholeText()).replaceAll(" ");
            if (afterSpace && collapsed.startsWith(" ")) {
                collapsed = collapsed.substring(1);
            }
            if (collapsed.isEmpty()) {
                text.text("");
                return;
            }
            String startSpace = !started && collapsed.startsWith(" ") ? " " : "";
            String word = collapsed.substring(startSpace.length());
            text.text(startSpace + word.replace(" ", NO_BREAK_SPACE));
            afterSpace = collapsed.endsWith(" ");
            started |= !word.isBlank();
            endSpace = afterSpace ? text : null;
        }

        private void child(@NotNull Element child) {
            if (isHidden(child)) {
                child.remove();
                return;
            }
            String whiteSpace = whiteSpace(child);
            if (IMG.equals(child.normalName())) {
                Element space = imageSpace(child);
                if (space != null) {
                    child.replaceWith(space);
                }
                content(false);
            } else if (whiteSpace.isEmpty() || INHERITED.contains(whiteSpace) || CssProp.WHITE_SPACE_NOWRAP_VALUE.equals(whiteSpace)) {
                mark(child);
            } else {
                wrapsAgain(child, whiteSpace);
            }
        }

        private void wrapsAgain(@NotNull Element child, @NotNull String whiteSpace) {
            CSSDeclarationList style = CssUtils.parseDeclarations(child.attr(HtmlTagAttr.STYLE));
            if (!wordWrap.isEmpty() && CssUtils.getPropertyValue(style, ECSSProperty.WORD_WRAP).isEmpty()) {
                CssUtils.setPropertyValue(style, ECSSProperty.WORD_WRAP, wordWrap);
                child.attr(HtmlTagAttr.STYLE, style.getAsCSSString());
            }
            if (afterSpace && CssProp.WHITE_SPACE_NORMAL_VALUE.equals(whiteSpace)) {
                dropStartSpace(child);
            }
            String wrapped = child.wholeText();
            if (!wrapped.isBlank()) {
                content(Character.isWhitespace(wrapped.charAt(wrapped.length() - 1)));
            }
        }

        /** Drops the space the element starts with, which collapses with the space before it, as its text wraps as normal. */
        private static void dropStartSpace(@NotNull Element element) {
            for (TextNode text : element.nodeStream(TextNode.class).toList()) {
                String rest = LEADING_WHITESPACE.matcher(text.getWholeText()).replaceFirst("");
                text.text(rest);
                if (!rest.isEmpty()) {
                    return;
                }
            }
        }

        /** Content other than the text of the word follows, which ends in a space or not. */
        private void content(boolean endsInSpace) {
            afterSpace = endsInSpace;
            started = true;
            endSpace = null;
        }

        /** Turns a space which ends the word back into a regular one. */
        private void end() {
            if (endSpace != null) {
                String text = endSpace.getWholeText();
                endSpace.text(text.substring(0, text.length() - 1) + " ");
            }
        }
    }

    /** The {@code word-wrap} the element has, of itself or of the nearest element around which states one. */
    private static @NotNull String wordWrap(@NotNull Element element) {
        for (Element current = element; current != null; current = current.parent()) {
            String wordWrap = CssUtils.getPropertyValue(CssUtils.parseDeclarations(current.attr(HtmlTagAttr.STYLE)), ECSSProperty.WORD_WRAP);
            if (!wordWrap.isEmpty()) {
                return CssProp.WORD_WRAP_NORMAL_VALUE.equals(wordWrap) ? "" : wordWrap;
            }
        }
        return "";
    }

    private static boolean isHidden(@NotNull Element element) {
        return CssProp.DISPLAY_NONE_VALUE.equals(CssUtils.getPropertyValue(CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE)), ECSSProperty.DISPLAY));
    }

    private static @NotNull String whiteSpace(@NotNull Element element) {
        return CssUtils.getPropertyValue(CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE)), ECSSProperty.WHITE_SPACE);
    }

    /**
     * An element which takes the room of the image in a word. It keeps the style of the image, as its margin, border and
     * padding, and holds an empty element as wide as the image through its padding, which goes with the word, and as tall
     * through its line height. None for an image whose size the measure only knows as it lays it out, which then stays as
     * it is and breaks the word.
     */
    private static @Nullable Element imageSpace(@NotNull Element image) {
        int[] size = imageSize(image);
        if (size == null) {
            return null;
        }
        String content = CssUtils.declaration(ECSSProperty.PADDING_LEFT, size[0] + "px");
        if (size[1] > 0) {
            content += " " + CssUtils.declaration(ECSSProperty.LINE_HEIGHT, size[1] + "px");
        }
        CSSDeclarationList box = CssUtils.parseDeclarations(image.attr(HtmlTagAttr.STYLE));
        CssUtils.removeProperty(box, ECSSProperty.WIDTH);
        CssUtils.removeProperty(box, ECSSProperty.HEIGHT);
        Element space = new Element("span").attr(LANG, NO_BREAK_LANGUAGE);
        if (!box.getAllDeclarations().isEmpty()) {
            space.attr(HtmlTagAttr.STYLE, box.getAsCSSString());
        }
        return space.appendChild(new Element("span").attr(HtmlTagAttr.STYLE, content).attr(LANG, NO_BREAK_LANGUAGE));
    }

    /**
     * The width and the height the measure lays an image out at: those its CSS states, else those of an image embedded in
     * its source, scaled to the one its CSS states. None when a size is relative, as to the font, or when neither states
     * one, as a stylesheet may.
     */
    private static int @Nullable [] imageSize(@NotNull Element image) {
        CSSDeclarationList style = CssUtils.parseDeclarations(image.attr(HtmlTagAttr.STYLE));
        Integer width = pixels(CssUtils.getPropertyValue(style, ECSSProperty.WIDTH), image.attr(HtmlTagAttr.WIDTH));
        Integer height = pixels(CssUtils.getPropertyValue(style, ECSSProperty.HEIGHT), image.attr(HtmlTagAttr.HEIGHT));
        if (Objects.equals(width, RELATIVE) || Objects.equals(height, RELATIVE)) {
            return null;
        }
        String src = image.attr(HtmlTagAttr.SRC);
        BufferedImage embedded = ImageUtil.isEmbeddedBase64Image(src) ? ImageUtil.loadEmbeddedBase64Image(src) : null;
        if (embedded == null || embedded.getWidth() <= 0 || embedded.getHeight() <= 0 || width != null && height != null) {
            return width == null && height == null ? null : new int[] {width != null ? width : 0, height != null ? height : 0};
        }
        if (width != null) {
            return new int[] {width, Math.round((float) embedded.getHeight() * width / embedded.getWidth())};
        }
        if (height != null) {
            return new int[] {Math.round((float) embedded.getWidth() * height / embedded.getHeight()), height};
        }
        return new int[] {embedded.getWidth(), embedded.getHeight()};
    }

    /**
     * A length in pixels, from CSS in an absolute unit or else from an attribute, which states it without a unit: none
     * when neither states it, {@link #RELATIVE} when it is relative or unknown.
     */
    private static @Nullable Integer pixels(@NotNull String css, @NotNull String attribute) {
        String value = css.trim();
        if (!value.isEmpty()) {
            if (CCSSValue.AUTO.equalsIgnoreCase(value)) {
                return null;
            }
            Matcher length = CSS_LENGTH.matcher(value);
            return length.matches() ? Math.round(Float.parseFloat(length.group(1)) * PIXELS_PER_UNIT.get(length.group(2).toLowerCase(Locale.ROOT))) : RELATIVE;
        }
        if (attribute.isBlank()) {
            return null;
        }
        try {
            return Math.round(Float.parseFloat(attribute.trim()));
        } catch (NumberFormatException e) {
            return RELATIVE;
        }
    }

    /**
     * Lets the measure break a word where the document lets it break anywhere or hyphenate it. The measure knows neither
     * {@code overflow-wrap} nor {@code hyphens}, only the older name {@code word-wrap}, and lays out its
     * {@code break-word} as CSS lays out {@code anywhere}: a word which hyphenates breaks at a syllable, which this comes
     * close to.
     */
    private static void measureBreakableWords(@NotNull Element table) {
        for (Element element : table.select("[style]")) {
            CSSDeclarationList style = CssUtils.parseDeclarations(element.attr(HtmlTagAttr.STYLE));
            if (CssProp.OVERFLOW_WRAP_ANYWHERE_VALUE.equals(CssUtils.getPropertyValue(style, ECSSProperty.OVERFLOW_WRAP))
                    || CCSSValue.AUTO.equals(CssUtils.getPropertyValue(style, ECSSProperty.HYPHENS))) {
                CssUtils.setPropertyValue(style, ECSSProperty.WORD_WRAP, CssProp.WORD_WRAP_BREAK_WORD_VALUE);
                element.attr(HtmlTagAttr.STYLE, style.getAsCSSString());
            }
        }
    }

    /**
     * The text styles the table inherits from the elements around it, which the measure would otherwise lay it out without:
     * of each, the one stated on the nearest of them.
     */
    private String inheritedTextStyle(@NotNull Element tableElement) {
        StringBuilder style = new StringBuilder();
        for (ECSSProperty property : INHERITED_TEXT_PROPERTIES) {
            for (Element ancestor : tableElement.parents()) {
                String value = CssUtils.getPropertyValue(CssUtils.parseDeclarations(ancestor.attr(HtmlTagAttr.STYLE)), property);
                if (!value.isEmpty()) {
                    style.append(CssUtils.declaration(property, value)).append(' ');
                    break;
                }
            }
        }
        return style.toString().trim();
    }

    private Box render(@NotNull Document doc, int pageWidth) {
        // What Graphics2DRenderer does, with a user agent of our own: it reads the stylesheets as the document is set,
        // so one set afterwards comes too late for them.
        XHTMLPanel panel = new XHTMLPanel(new OfflineUserAgent());
        panel.setInteractive(false);
        panel.setDocument(doc, "");
        SharedContext sharedContext = panel.getSharedContext();

        // This is a throwaway measurement-only layout over an isolated shell document with an empty base URL, which loads
        // nothing: only images embedded in their source are drawn (see SourceAwareReplacedElementFactory). Must be set
        // before the layout.
        ReplacedElementFactory defaultFactory = sharedContext.getReplacedElementFactory();
        sharedContext.setReplacedElementFactory(new SourceAwareReplacedElementFactory(defaultFactory));

        useMeasurementFont(sharedContext);
        sharedContext.setLineBreakingStrategy(new MeasureLineBreakingStrategy());
        // A size in points becomes pixels at the resolution of the screen, which is 72 dpi on a headless server and
        // anything on a desktop. WeasyPrint, as CSS, counts 96 pixels to an inch.
        sharedContext.setDPI(CSS_DPI);

        BufferedImage image = new BufferedImage(pageWidth, PAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setFont(EMBEDDED_FONT);

            panel.setSize(new Dimension(pageWidth, PAGE_HEIGHT));
            panel.doDocumentLayout(g2d);
            return panel.getRootBox();
        } finally {
            // Explicitly dispose the Graphics2D and flush the BufferedImage to release resources
            g2d.dispose();
            image.flush();
        }
    }

    private void findTableAndAnalyze(Box box, Map<Integer, Integer> columnWidths, Gathered gathered, List<Integer> rowHeights) {
        if (box == null) {
            return;
        }

        // Check if this is a table box
        if (box.getElement() != null && TABLE.equalsIgnoreCase(box.getElement().getNodeName())) {
            gatherColumnWidths(box, columnWidths, gathered, rowHeights);
            return; // Found and analyzed, no need to go deeper
        }

        // Recursively search children
        if (box instanceof LineBox lineBox) {
            for (Box inlinedBox : lineBox.getNonFlowContent()) {
                findTableAndAnalyze(inlinedBox, columnWidths, gathered, rowHeights);
            }
        } else {
            for (int i = 0; i < box.getChildCount(); i++) {
                findTableAndAnalyze(box.getChild(i), columnWidths, gathered, rowHeights);
            }
        }
    }

    private void gatherColumnWidths(@NotNull Box tableBox, @NotNull Map<Integer, Integer> columnWidths, @NotNull Gathered gathered, @NotNull List<Integer> rowHeights) {
        gathered.tableWidth = tableBox.getWidth();
        List<Box> tbody = findChildrenByTag(tableBox, TBODY);
        List<Box> rows = findChildrenByTag(!tbody.isEmpty() ? tbody.getFirst() : tableBox, TR);

        // Analyze all rows to properly handle colspan
        for (Box row : rows) {
            rowHeights.add(row.getHeight());
            List<Box> cells = findChildrenByTag(row, TD, TH);
            if (!cells.isEmpty() && cells.stream().allMatch(cell -> TH.equalsIgnoreCase(cell.getElement().getNodeName()))) {
                // A row of header cells is repeated on every page the table spans, so it takes its height there
                gathered.headerHeight += row.getHeight();
            }
            int columnIndex = 0;

            for (Box cell : cells) {
                int colspan = getColspan(cell);
                int cellWidth = cell.getContentWidth();

                if (colspan == 1) {
                    addColumnWidth(columnWidths, columnIndex, cellWidth);
                    columnIndex++;
                } else {
                    // Multi-column cell - distribute width proportionally
                    int widthPerColumn = cellWidth / colspan;
                    for (int i = 0; i < colspan; i++) {
                        addColumnWidth(columnWidths, columnIndex + i, widthPerColumn);
                    }
                    columnIndex += colspan;
                }
            }
        }
    }

    private void addColumnWidth(@NotNull Map<Integer, Integer> columnWidths, int index, int width) {
        if (!columnWidths.containsKey(index)) {
            columnWidths.put(index, width);
        } else {
            // Update with maximum width seen
            columnWidths.put(index, Math.max(columnWidths.get(index), width));
        }
    }

    private int getColspan(@NotNull Box cell) {
        if (cell.getElement() != null && cell.getElement().hasAttribute("colspan")) {
            try {
                return Integer.parseInt(cell.getElement().getAttribute("colspan"));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    private List<Box> findChildrenByTag(Box parent, String... tagNames) {
        List<Box> result = new ArrayList<>();
        Set<String> tags = new HashSet<>(Arrays.asList(tagNames));

        for (int i = 0; i < parent.getChildCount(); i++) {
            Box child = parent.getChild(i);
            // When no tbody exists and rows are included directly to table element, rows are enclosed into artificial TableSectionBox which we should just skip and go deeper into hierarchy
            if (child instanceof TableSectionBox tableSectionBox) {
                result.addAll(findChildrenByTag(tableSectionBox, tagNames));
            } else if (child.getElement() != null && tags.contains(child.getElement().getNodeName().toLowerCase())) {
                result.add(child);
            }
        }
        return result;
    }

    private Map<Integer, Integer> adjustWidths(Map<Integer, Integer> renderedWidths, int pageWidth) {
        int renderedWidth = renderedWidths.values().stream().reduce(0, Integer::sum);
        Map<Integer, Integer> adjustedWidths = new HashMap<>();
        if (renderedWidth > 0) {
            float adjustingRatio = (float) pageWidth / renderedWidth;
            for (Map.Entry<Integer, Integer> entry : renderedWidths.entrySet()) {
                adjustedWidths.put(entry.getKey(), (int) (entry.getValue() * adjustingRatio));
            }
        }
        return adjustedWidths;
    }

    /**
     * Hands the font the measurement ships with to the layout by name, so the file a machine happens to have installed
     * under the name of that font is never the one which lays the table out.
     * <p>
     * The renderer takes no resolver of its own, and a font given by {@code setFontMapping} gets a derived bold, so the
     * resolver is set, as the one way the renderer allows.
     * </p>
     */
    @SuppressWarnings({"removal", "java:S5738"})
    private static void useMeasurementFont(@NotNull SharedContext sharedContext) {
        sharedContext.setFontResolver(new MeasurementFontResolver());
    }

    /**
     * Lays the measurement font out in the face its weight and style ask for. The resolver of flying-saucer derives a
     * bold or an italic face from the one font it is given, and a derived bold is wider or narrower than the real one.
     * <p>
     * The font kerns, as WeasyPrint does: a pair as "AV" takes less than its letters added up. Without kerning a line
     * measures up to 5 % wider than it prints.
     * </p>
     */
    static class MeasurementFontResolver extends AWTFontResolver {
        private static final Map<TextAttribute, Object> KERNING = Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON);

        @Override
        protected Font resolveFont(SharedContext ctx, String font, float size, IdentValue weight, IdentValue style, IdentValue variant) {
            if (!MEASUREMENT_FONT_FAMILY.equals(font.replace("'", "").replace("\"", ""))) {
                return super.resolveFont(ctx, font, size, weight, style, variant);
            }
            boolean bold = weight == IdentValue.BOLD || weight == IdentValue.FONT_WEIGHT_700 || weight == IdentValue.FONT_WEIGHT_800 || weight == IdentValue.FONT_WEIGHT_900;
            boolean italic = style == IdentValue.ITALIC || style == IdentValue.OBLIQUE;
            return face(bold, italic).deriveFont(size * ctx.getTextRenderer().getFontScale()).deriveFont(KERNING);
        }

        private static @NotNull Font face(boolean bold, boolean italic) {
            if (bold) {
                return italic ? EMBEDDED_BOLD_ITALIC_FONT : EMBEDDED_BOLD_FONT;
            }
            return italic ? EMBEDDED_ITALIC_FONT : EMBEDDED_FONT;
        }
    }

    /**
     * A {@link ReplacedElementFactory} for the measurement-only pre-render which loads no image.
     * <p>
     * The default factory loads an image from its source itself, over the network and without a timeout. Here only an
     * image embedded in its source goes to it, and is drawn at its size. Any other image is laid out as the default
     * factory lays out one which failed to load, at the size its CSS states, without an attempt to load it.
     * </p>
     * <p>
     * flying-saucer's default factory tries to build a "missing image" placeholder for an {@code <img>} whose
     * source is absent/empty. When the element additionally has no explicit width/height both CSS dimensions
     * resolve to {@code -1}, and the placeholder construction throws {@code IllegalArgumentException} internally,
     * which is swallowed but logged at ERROR with a full stacktrace. Such images are returned as an empty
     * element here so that buggy path is never reached. Everything else which is not an image (form controls, ...)
     * is delegated to the default factory.
     * </p>
     */
    static class SourceAwareReplacedElementFactory implements ReplacedElementFactory {
        private final ReplacedElementFactory delegate;

        SourceAwareReplacedElementFactory(ReplacedElementFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {
            org.w3c.dom.Element element = box.getElement();
            if (element != null && c.getNamespaceHandler().isImageElement(element)) {
                String src = c.getNamespaceHandler().getImageSourceURI(element);
                if (src == null || src.isBlank()) {
                    // No usable source in this measurement-only pass: avoid the -1x-1 placeholder attempt.
                    return new EmptyReplacedElement(Math.max(cssWidth, 0), Math.max(cssHeight, 0));
                }
                if (!ImageUtil.isEmbeddedBase64Image(src)) {
                    // What the default factory makes of an image it could not load, without loading it: the transparent
                    // image of a failed load, at the size the CSS states
                    AWTFSImage unloaded = (AWTFSImage) ImageResourceLoader.createImageResource(src, null).getImage();
                    return new InstantImageReplacedElement(unloaded.getImage(), cssWidth, cssHeight);
                }
            }
            return delegate.createReplacedElement(c, box, uac, cssWidth, cssHeight);
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public void remove(org.w3c.dom.Element e) {
            delegate.remove(e);
        }

        @Override
        public void setFormSubmissionListener(FormSubmissionListener listener) {
            delegate.setFormSubmissionListener(listener);
        }
    }

    /**
     * A user agent which opens nothing but the stylesheet flying-saucer ships, which makes a table a table. The measure
     * lays out a table alone, so a stylesheet the table imports is not loaded, over the network or from a file. The
     * images are left out by {@link SourceAwareReplacedElementFactory}, as the factory loads them itself.
     */
    static class OfflineUserAgent extends NaiveUserAgent {
        private static final String DEFAULT_STYLESHEET = new XhtmlNamespaceHandler().getDefaultStylesheet().map(StylesheetInfo::getUri).orElse("");

        @Override
        protected InputStream openStream(String uri) throws IOException {
            if (isDefaultStylesheet(uri)) {
                return super.openStream(uri);
            }
            throw new IOException("The table measurement loads no resources: " + uri);
        }

        private static boolean isDefaultStylesheet(String uri) {
            return !DEFAULT_STYLESHEET.isEmpty() && DEFAULT_STYLESHEET.equals(uri);
        }
    }

    /**
     * The places a line of the measure may break at: those flying-saucer finds, but none between a hyphen and a digit, as
     * the PDF is laid out by WeasyPrint. WeasyPrint follows the default rule of UAX #14 (LB25, {@code HY × NU}) and lays
     * out an ID or a date such as {@code REQ-1001} or {@code 2022-01-05} whole. flying-saucer breaks after the hyphen, as
     * CSS Text 3 (5.2) suggests for browsers, and finds the column narrower than it prints.
     */
    static final class MeasureLineBreakingStrategy implements LineBreakingStrategy {
        @Override
        public BreakPointsProvider getBreakPointsProvider(String text, String lang, CalculatedStyle style) {
            return getBreakPointsProvider(text, 0, lang, style);
        }

        /**
         * The break points of the text from {@code start} on, at offsets from {@code start}. Found without a copy of the
         * rest of the text, which flying-saucer asks for at the start of each line: the copies cost a long cell time
         * quadratic in its length.
         */
        @Override
        public BreakPointsProvider getBreakPointsProvider(String text, int start, String lang, CalculatedStyle style) {
            if (NO_BREAK_LANGUAGE.equals(lang)) {
                return afterSpaces(text, start);
            }
            BreakIterator iterator = new UrlAwareLineBreakIterator(text, start);
            return () -> {
                int next = iterator.next();
                while (next != BreakIterator.DONE && isBetweenHyphenAndDigit(text, start, next)) {
                    next = iterator.next();
                }
                return next == BreakIterator.DONE ? BreakPoint.DONE : new BreakPoint(next - start);
            };
        }

        /**
         * The places after the spaces of a text which may not wrap, at offsets from {@code start}. Its own spaces are
         * no-break spaces in the measure, so a space is one of the text before it, where the line may break.
         */
        private static @NotNull BreakPointsProvider afterSpaces(@NotNull String text, int start) {
            int[] offset = {start};
            return () -> {
                while (offset[0] < text.length()) {
                    if (text.charAt(offset[0]++) == ' ' && (offset[0] == text.length() || text.charAt(offset[0]) != ' ')) {
                        return new BreakPoint(offset[0] - start);
                    }
                }
                return BreakPoint.DONE;
            };
        }

        private static boolean isBetweenHyphenAndDigit(@NotNull String text, int start, int offset) {
            return offset > start && offset < text.length() && text.charAt(offset - 1) == '-' && Character.isDigit(text.codePointAt(offset));
        }
    }
}
