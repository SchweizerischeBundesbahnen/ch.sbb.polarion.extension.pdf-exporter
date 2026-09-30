package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.helger.css.decl.CSSDeclarationList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps a short table row on one page.
 * <p>
 * Split by the end of a page, a short row leaves its first cells on one page and the rest of it on the next: an ID above
 * nothing, and a title below no ID. CSS can ask for every row to stay whole, but a row kept whole moves to the next page
 * and leaves the rest of this one empty, a whole page for a row taller than a page. So each table is measured, and only
 * the rows short enough to leave little empty are kept whole. A taller row breaks where the page ends, with plenty of
 * it on either side.
 * </p>
 */
public class TableRowsAdjuster extends AbstractAdjuster {

    /**
     * The share of a page a row may take and still be kept whole: moved to the next page, it leaves no more than that
     * empty on this one.
     */
    private static final float SHARE_OF_PAGE = 0.25f;

    /**
     * What the measure cannot size: an image is not in the document yet, only the address it comes from. Icons are the
     * exception, a line high whatever they show.
     */
    private static final String EMBEDDED_CONTENT = "svg, object, iframe";

    /**
     * Marks an icon whose rows are measured once the images are embedded, when its address no longer says it is one.
     */
    private static final String ICON_MARK = "data-icon";

    /**
     * The icons Polarion draws in a line of text: of a link, of an enum value, or one of its own image folders. Or one
     * marked as an icon before its address was replaced.
     */
    private static final String ICON = "img.polarion-Icons, .polarion-JSEnumOption img, img[src*=/icons/], img[src*=/ria/images/], img[" + ICON_MARK + "]";

    private static final String ROWS_OF_THE_TABLE = "> tr, > thead > tr, > tbody > tr, > tfoot > tr";

    private static final String CELL = "td, th";

    /** The height of the page the rows are measured against. */
    private final int pageHeight;

    public TableRowsAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this(document, conversionParams, PaperSizeUtils.getMaxHeight(conversionParams));
    }

    /**
     * @param pageHeight the height of the page the rows are measured against, lower than the one the parameters name
     *                   where a part of the document may be laid out in the other orientation
     */
    public TableRowsAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams, int pageHeight) {
        super(document, conversionParams);
        this.pageHeight = pageHeight;
    }

    @Override
    public void execute() {
        int pageWidth = PaperSizeUtils.getMaxWidth(conversionParams);
        // The tables come in document order, so a table around another one is measured before it
        Map<Element, TableAnalyzer.TableMetrics> measured = new IdentityHashMap<>();
        for (Element table : document.select(HtmlTag.TABLE)) {
            Elements rows = table.select(ROWS_OF_THE_TABLE);
            Integer width = widthOf(table, measured, pageWidth);
            if (!rows.isEmpty() && width != null) {
                TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, width);
                measured.put(table, metrics);
                keepShortRowsWhole(rows, metrics);
            }
        }
        document.select("img[" + ICON_MARK + "]").removeAttr(ICON_MARK);
    }

    /**
     * Marks the icons of a document whose rows are measured once its images are embedded, which replaces the address
     * an icon is known by. Measured, the rows lose the marks.
     */
    public static void markIcons(@NotNull Document document) {
        document.select(ICON).attr(ICON_MARK, "");
    }

    private void keepShortRowsWhole(@NotNull Elements rows, @NotNull TableAnalyzer.TableMetrics metrics) {
        List<Integer> rowHeights = metrics.rowHeights();
        if (rowHeights.size() != rows.size()) {
            // The measure saw another table than the document holds, so it says nothing about these rows
            return;
        }
        float allowedHeight = (pageHeight - metrics.headerHeight()) * SHARE_OF_PAGE;
        for (int index = 0; index < rows.size(); index++) {
            Element row = rows.get(index);
            if (rowHeights.get(index) <= allowedHeight && isMeasured(row)) {
                keepWhole(row);
            }
        }
    }

    /**
     * The width a table is laid out at: the page for a table of its own, the column of its cell for a nested one. Null
     * when the width of that column is not known, the table around it not measured.
     */
    private static @Nullable Integer widthOf(@NotNull Element table, @NotNull Map<Element, TableAnalyzer.TableMetrics> measured, int pageWidth) {
        Element cell = table.parent() != null ? table.parent().closest(CELL) : null;
        if (cell == null) {
            return pageWidth;
        }
        Element outerTable = cell.closest(HtmlTag.TABLE);
        TableAnalyzer.TableMetrics outer = outerTable != null ? measured.get(outerTable) : null;
        if (outer == null) {
            return null;
        }
        int column = 0;
        for (Element before = cell.previousElementSibling(); before != null; before = before.previousElementSibling()) {
            column += span(before);
        }
        int width = 0;
        for (int index = column; index < column + span(cell); index++) {
            Integer columnWidth = outer.columnWidths().get(index);
            if (columnWidth == null) {
                return null;
            }
            width += columnWidth;
        }
        return width > 0 ? width : null;
    }

    private static int span(@NotNull Element cell) {
        try {
            return Math.max(1, Integer.parseInt(cell.attr("colspan").trim()));
        } catch (NumberFormatException e) {
            return 1; // A cell without a readable colspan takes one column
        }
    }

    /** Whether the measure knows the height of the row: no content it cannot size, and no image but icons. */
    private static boolean isMeasured(@NotNull Element row) {
        return row.select(EMBEDDED_CONTENT).isEmpty() && row.select(HtmlTag.IMG).size() == row.select(ICON).size();
    }

    /** A row which states how it breaks itself is left as it is. */
    private void keepWhole(@NotNull Element row) {
        CSSDeclarationList rowStyles = CssUtils.parseDeclarations(row.attr(HtmlTagAttr.STYLE));
        if (!CssUtils.getPropertyValue(rowStyles, CssProp.BREAK_INSIDE).isEmpty() || !CssUtils.getPropertyValue(rowStyles, CssProp.PAGE_BREAK_INSIDE).isEmpty()) {
            return;
        }
        CssUtils.setPropertyValue(rowStyles, CssProp.BREAK_INSIDE, CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE);
        row.attr(HtmlTagAttr.STYLE, rowStyles.getAsCSSString());
    }
}
