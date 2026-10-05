package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import com.helger.css.property.ECSSProperty;
import com.helger.css.propertyvalue.CCSSValue;
import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import com.helger.css.decl.CSSDeclarationList;
import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.xhtmlrenderer.context.AWTFontResolver;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.newtable.TableSectionBox;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.simple.Graphics2DRenderer;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.EmptyReplacedElement;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class TableAnalyzer {
    private static final Logger logger = Logger.getLogger(TableAnalyzer.class);

    static {
        // flying-saucer ships a broken default for "xr.text.aa-rendering-hint"
        // ("RenderingHints.VALUE_TEXT_ANTIALIAS_HGRB" - unqualified class name, non-existent constant), so
        // Java2DTextRenderer can never resolve it and instead queries the AWT desktop font hints. On a headless
        // server those are null, the subsequent map lookup throws an NPE which flying-saucer swallows but logs at
        // WARN with a full stack trace - once per renderer, i.e. once per table we measure. Pin a valid,
        // fully-qualified constant before flying-saucer's Configuration singleton is first read (it absorbs xr.*
        // system properties at init, and TableAnalyzer is this project's only flying-saucer entry point) so the
        // desktop-hints path - and its noise - is never reached, keeping anti-aliasing deterministic too.
        System.setProperty("xr.text.aa-rendering-hint", "java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON");
        // Without fractional metrics each glyph is as wide as the rasterizer of the platform hints it, rounded to a
        // pixel: the same font measures narrower on Linux than on macOS. Fractional metrics take the widths of the
        // font itself, as WeasyPrint does.
        System.setProperty("xr.text.fractional-font-metrics", "true");
    }

    private static final String TABLE = "table";
    private static final String TBODY = "tbody";
    private static final String TR = "tr";
    private static final String TD = "td";
    private static final String TH = "th";

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
        tempDoc.body().appendElement("div").attr(HtmlTagAttr.STYLE, inheritedTextStyle(tableElement)).appendChild(table);
        return new W3CDom().fromJsoup(tempDoc);
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
                    style.append(property.getName()).append(": ").append(value).append("; ");
                    break;
                }
            }
        }
        return style.toString().trim();
    }

    private Box render(@NotNull Document doc, int pageWidth) {
        Graphics2DRenderer renderer = new Graphics2DRenderer(doc, "");

        // This is a throwaway measurement-only layout over an isolated shell document with an empty base URL,
        // so image sources are not resolvable here anyway. Source-less images would otherwise drive
        // flying-saucer into building a -1x-1 placeholder image, which throws internally and gets logged at
        // ERROR (see SwingReplacedElementFactory#newIrreplaceableImageElement). Short-circuit those images to
        // an empty element while delegating healthy images to the default factory so they still contribute
        // their intrinsic width to the measurement. Must be set before layout().
        ReplacedElementFactory defaultFactory = renderer.getSharedContext().getReplacedElementFactory();
        renderer.getSharedContext().setReplacedElementFactory(new SourceAwareReplacedElementFactory(defaultFactory));

        useMeasurementFont(renderer.getSharedContext());
        // A size in points becomes pixels at the resolution of the screen, which is 72 dpi on a headless server and
        // anything on a desktop. WeasyPrint, as CSS, counts 96 pixels to an inch.
        renderer.getSharedContext().setDPI(CSS_DPI);

        BufferedImage image = new BufferedImage(pageWidth, PAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setFont(EMBEDDED_FONT);

            Dimension dim = new Dimension(pageWidth, PAGE_HEIGHT);
            renderer.layout(g2d, dim);
            return renderer.getPanel().getRootBox();
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
     */
    static class MeasurementFontResolver extends AWTFontResolver {
        @Override
        protected Font resolveFont(SharedContext ctx, String font, float size, IdentValue weight, IdentValue style, IdentValue variant) {
            if (!MEASUREMENT_FONT_FAMILY.equals(font.replace("'", "").replace("\"", ""))) {
                return super.resolveFont(ctx, font, size, weight, style, variant);
            }
            boolean bold = weight == IdentValue.BOLD || weight == IdentValue.FONT_WEIGHT_700 || weight == IdentValue.FONT_WEIGHT_800 || weight == IdentValue.FONT_WEIGHT_900;
            boolean italic = style == IdentValue.ITALIC || style == IdentValue.OBLIQUE;
            return face(bold, italic).deriveFont(size * ctx.getTextRenderer().getFontScale());
        }

        private static @NotNull Font face(boolean bold, boolean italic) {
            if (bold) {
                return italic ? EMBEDDED_BOLD_ITALIC_FONT : EMBEDDED_BOLD_FONT;
            }
            return italic ? EMBEDDED_ITALIC_FONT : EMBEDDED_FONT;
        }
    }

    /**
     * A {@link ReplacedElementFactory} for the measurement-only pre-render that neutralises source-less images.
     * <p>
     * flying-saucer's default factory tries to build a "missing image" placeholder for an {@code <img>} whose
     * source is absent/empty. When the element additionally has no explicit width/height both CSS dimensions
     * resolve to {@code -1}, and the placeholder construction throws {@code IllegalArgumentException} internally,
     * which is swallowed but logged at ERROR with a full stacktrace. Such images are returned as an empty
     * element here so that buggy path is never reached. Everything else (healthy images, form controls, ...)
     * is delegated to the default factory so it still contributes its real intrinsic width to the measurement.
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
}
