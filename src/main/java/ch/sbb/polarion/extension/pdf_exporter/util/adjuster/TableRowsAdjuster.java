package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.helger.css.decl.CSSDeclarationList;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

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

    /** The icons Polarion draws in a line of text: of a link, of an enum value, or one of its own image folders. */
    private static final String ICON = "img.polarion-Icons, .polarion-JSEnumOption img, img[src*=/icons/], img[src*=/ria/images/]";

    private static final String ROWS_OF_THE_TABLE = "> tr, > thead > tr, > tbody > tr, > tfoot > tr";

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
        for (Element table : document.select(HtmlTag.TABLE)) {
            Elements rows = table.select(ROWS_OF_THE_TABLE);
            // A nested table is measured with the row which holds it, at the width of its cell: that row is kept whole or not
            if (rows.isEmpty() || table.parents().stream().anyMatch(ancestor -> ancestor.nameIs(HtmlTag.TABLE))) {
                continue;
            }
            TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, PaperSizeUtils.getMaxWidth(conversionParams));
            List<Integer> rowHeights = metrics.rowHeights();
            if (rowHeights.size() != rows.size()) {
                // The measure saw another table than the document holds, so it says nothing about these rows
                continue;
            }
            float allowedHeight = (pageHeight - metrics.headerHeight()) * SHARE_OF_PAGE;
            for (int index = 0; index < rows.size(); index++) {
                Element row = rows.get(index);
                if (rowHeights.get(index) <= allowedHeight && isMeasured(row)) {
                    keepWhole(row);
                }
            }
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
