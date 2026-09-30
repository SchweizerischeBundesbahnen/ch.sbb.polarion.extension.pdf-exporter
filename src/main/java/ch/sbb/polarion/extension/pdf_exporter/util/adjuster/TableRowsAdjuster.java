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
 * Keeps a table row on one page where it fits one.
 * <p>
 * Split by the end of a page, a row leaves its first cells on one page and the rest of it on the next: an ID above
 * nothing, and a title below no ID. CSS can ask for every row to stay whole, but a row taller than a page then moves
 * to a page of its own and leaves the page before it empty. So each table is measured, and only the rows which fit a
 * page, under the header the table repeats there, are kept whole. A taller one still breaks where the page ends.
 * </p>
 */
public class TableRowsAdjuster extends AbstractAdjuster {

    /**
     * The share of a page a row may take and still be kept whole. The measure lays the table out in a font of its own,
     * not the one of the PDF, so a row near the height of a page is left free to break rather than risk a page of its own.
     */
    private static final float SHARE_OF_PAGE = 0.9f;

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
            if (rows.isEmpty()) {
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
                if (rowHeights.get(index) <= allowedHeight) {
                    keepWhole(rows.get(index));
                }
            }
        }
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
