package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import com.helger.css.decl.CSSDeclarationList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public class ImageSizeInTablesAdjuster extends AbstractAdjuster {

    private static final String TD_TH_SELECTOR = String.format("%s, %s", HtmlTag.TD, HtmlTag.TH);

    /** The rows of the table itself, in the order its measure lists them, and not those of a table nested in it. */
    private static final String ROWS_OF_THE_TABLE = "> tr, > thead > tr, > tbody > tr, > tfoot > tr";

    /** What a cell adds around the image it holds: the padding Polarion writes and the border of the cell. */
    private static final int CELL_CHROME_PX = 16;

    /** However tall a header grows, an image is still worth seeing. */
    private static final int MIN_IMAGE_HEIGHT_PX = 100;


    public ImageSizeInTablesAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        Elements tables = document.select(HtmlTag.TABLE);
        // A table holds the images of the tables nested in it too, and the first table to fit an image decides for its row.
        // The row belongs to the innermost table, which is measured for it once every image is fitted.
        Map<Element, List<Element>> rowsToKeepWhole = new LinkedHashMap<>();

        for (Element table : tables) {
            Elements images = table.select(HtmlTag.IMG);
            if (images.isEmpty()) {
                continue;
            }

            // Pre-render table and get rendered column widths proportionally adjusted to page width
            TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, PaperSizeUtils.getMaxWidth(conversionParams));
            Map<Integer, Integer> columnWidths = metrics.columnWidths();

            int allowedHeight = allowedHeight(metrics.headerHeight());
            for (Element img : images) {
                if (fitToColumn(img, columnWidths, allowedHeight)) {
                    addRowOf(img, rowsToKeepWhole);
                }
            }
        }
        rowsToKeepWhole.forEach(this::keepWholeWhereTheRestFits);
    }

    /** The measure reads the height an image states and not the limit put on it, so the image of a copy states the limit. */
    private void drawAtTheHeightItIsLimitedTo(@NotNull Element img) {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE));
        float limit = extractPixels(CssUtils.getPropertyValue(cssStyles, CssProp.MAX_HEIGHT));
        if (limit > 0 && statedSize(img, cssStyles, CssProp.HEIGHT) > limit) {
            CssUtils.setPropertyValue(cssStyles, CssProp.HEIGHT, ((int) limit) + Measure.PX);
            img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
            img.removeAttr(CssProp.HEIGHT);
        }
    }

    /** A row of header cells alone, which the table repeats on every page it spans. */
    private static boolean isHeaderRow(@NotNull Element row) {
        Elements cells = row.select("> " + HtmlTag.TD + ", > " + HtmlTag.TH);
        return !cells.isEmpty() && cells.stream().allMatch(cell -> HtmlTag.TH.equals(cell.tagName()));
    }

    /** Adds the row of the image to the rows of the table it belongs to, the innermost one. */
    private static void addRowOf(@NotNull Element img, @NotNull Map<Element, List<Element>> rowsByTable) {
        Element row = img.closest(HtmlTag.TR);
        Element table = row != null ? row.closest(HtmlTag.TABLE) : null;
        if (table != null) {
            List<Element> rows = rowsByTable.computeIfAbsent(table, key -> new ArrayList<>());
            if (!rows.contains(row)) {
                rows.add(row);
            }
        }
    }

    /**
     * Fits the image to its column and to the height the page leaves it.
     *
     * @return whether the image is drawn as tall as the page leaves it, and a row of that height leaves the header alone too
     */
    private boolean fitToColumn(@NotNull Element img, @NotNull Map<Integer, Integer> columnWidths, int allowedHeight) {
        float statedWidth = statedSize(img, CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE)), CssProp.WIDTH);
        float statedHeight = limitHeight(img, allowedHeight);

        float cssWidth = extractWidth(img, CssProp.WIDTH);
        float cssMaxWidth = extractWidth(img, CssProp.MAX_WIDTH);

        float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(img);
        float paramsBasedWidth = PaperSizeUtils.getMaxWidthInTables(conversionParams);

        float maxWidth = getMaxWidth(img, columnWidths, columnCountBasedWidth, paramsBasedWidth);

        if (cssWidth > maxWidth || cssMaxWidth > maxWidth) {
            adjustImageStyle(img, maxWidth, cssWidth);
            keepTheRowWholeUnlessTheImageStatesItsSize(img);
        }
        return drawnHeight(statedWidth, statedHeight, maxWidth) > allowedHeight;
    }

    /**
     * The height an image of the stated size is drawn at once the column narrows it, as the ratio it states.
     * An image which states no height has none to cut.
     */
    private static float drawnHeight(float statedWidth, float statedHeight, float maxWidth) {
        return statedWidth > 0 && statedWidth > maxWidth ? statedHeight * maxWidth / statedWidth : statedHeight;
    }

    /**
     * A row is kept whole where all the rest of it fits under the header as well. Text of another cell which runs
     * over a page splits the row anyway, and kept whole the row would only leave the page before it blank.
     */
    private void keepWholeWhereTheRestFits(@NotNull Element table, @NotNull List<Element> rows) {
        Element withoutImages = table.clone();
        Elements ownRows = table.select(ROWS_OF_THE_TABLE);
        Elements clonedRows = withoutImages.select(ROWS_OF_THE_TABLE);
        for (Element row : rows) {
            int index = ownRows.indexOf(row);
            // A header keeps its images, fitted to the page as they are drawn: the room under it is what is measured
            if (index >= 0 && index < clonedRows.size() && !isHeaderRow(row)) {
                clonedRows.get(index).select(HtmlTag.IMG).remove();
            }
        }
        withoutImages.select(HtmlTag.IMG).forEach(this::drawAtTheHeightItIsLimitedTo);
        TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(withoutImages, PaperSizeUtils.getMaxWidth(conversionParams));
        List<Integer> heights = metrics.rowHeights();
        int allowedHeight = allowedHeight(metrics.headerHeight());
        if (heights.size() != ownRows.size()) {
            // The measure saw another table than the document holds, so it says nothing about these rows
            return;
        }
        for (Element row : rows) {
            int index = ownRows.indexOf(row);
            if (index >= 0 && heights.get(index) <= allowedHeight) {
                keepWhole(row);
            }
        }
    }

    /**
     * An image wider than its column which states no size of its own takes the size of the file it comes
     * from, which can be a page tall. Such a row is kept whole: split, it leaves the image on the next page
     * and the header of its table on this one, above a row which shows nothing. A row holding an icon, or an
     * image of a stated size which the page holds, still breaks where it must, as any row of text does.
     */
    private void keepTheRowWholeUnlessTheImageStatesItsSize(Element img) {
        if (statedSize(img, CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE)), CssProp.WIDTH) > 0
                || statedSize(img, CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE)), CssProp.HEIGHT) > 0) {
            return;
        }
        keepTheRowWhole(img);
    }

    private void keepTheRowWhole(Element img) {
        Element row = img.closest(HtmlTag.TR);
        if (row != null) {
            keepWhole(row);
        }
    }

    /** Keeps the row on one page, unless the document already says how it may break. */
    private static void keepWhole(@NotNull Element row) {
        CSSDeclarationList rowStyles = CssUtils.parseDeclarations(row.attr(HtmlTagAttr.STYLE));
        if (!CssUtils.getPropertyValue(rowStyles, CssProp.BREAK_INSIDE).isEmpty() || !CssUtils.getPropertyValue(rowStyles, CssProp.PAGE_BREAK_INSIDE).isEmpty()) {
            return;
        }
        CssUtils.setPropertyValue(rowStyles, CssProp.BREAK_INSIDE, CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE);
        row.attr(HtmlTagAttr.STYLE, rowStyles.getAsCSSString());
    }

    /**
     * A row which fills the page to its last pixel cannot carry the header of its table: the header is then
     * left on the page before, above nothing, or dropped altogether. The image gives that height up.
     *
     * @return the height the image states, or 0 where it states none or no more than the page holds
     */
    private float limitHeight(Element img, int allowedHeight) {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE));
        float statedMaxHeight = extractPixels(CssUtils.getPropertyValue(cssStyles, CssProp.MAX_HEIGHT));
        if (statedMaxHeight > 0 && statedMaxHeight <= allowedHeight) {
            // The image asks for less than the page leaves it, and what it asks for is what it keeps
            return 0;
        }

        CssUtils.setPropertyValue(cssStyles, CssProp.MAX_HEIGHT, allowedHeight + Measure.PX);
        float statedHeight = statedSize(img, cssStyles, CssProp.HEIGHT);
        if (statedHeight > allowedHeight && CssUtils.getPropertyValue(cssStyles, CssProp.OBJECT_FIT).isEmpty()) {
            // The limit cuts into the height the image states, and a height cut alone squashes the drawing
            CssUtils.setPropertyValue(cssStyles, CssProp.OBJECT_FIT, CssProp.OBJECT_FIT_CONTAIN_VALUE);
        }
        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
        return statedHeight;
    }

    /** The height a page leaves an image of a row under the header of its table. */
    private int allowedHeight(int headerHeight) {
        return Math.max(PaperSizeUtils.getMaxHeight(conversionParams) - headerHeight - CELL_CHROME_PX, MIN_IMAGE_HEIGHT_PX);
    }

    /** The size the image states in pixels, from its style or from its attribute. */
    private float statedSize(Element img, CSSDeclarationList cssStyles, String property) {
        String value = CssUtils.getPropertyValue(cssStyles, property);
        return value.isEmpty() ? parseNumber(img.attr(property)) : extractPixels(value);
    }

    private float extractWidth(Element img, String property) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(style);

        String value = CssUtils.getPropertyValue(cssStyles, property);
        if (value.isEmpty()) {
            // An attribute states a width in pixels, and it is a width like any other
            return CssProp.WIDTH.equals(property) ? parseNumber(img.attr(CssProp.WIDTH)) : 0;
        }

        if (value.equals(CssProp.AUTO_VALUE)) {
            return Float.MAX_VALUE;
        }

        if (value.endsWith(Measure.PERCENT)) {
            return PaperSizeUtils.getMaxWidthInTables(conversionParams) * parseNumber(value.replace(Measure.PERCENT, ""));
        }
        return extractPixels(value);
    }

    /** The length in pixels, where it is stated in an absolute unit, and 0 otherwise. */
    private float extractPixels(String value) {
        for (Map.Entry<String, Float> unit : Measure.ABSOLUTE_UNITS_IN_PX.entrySet()) {
            if (value.endsWith(unit.getKey())) {
                return parseNumber(value.substring(0, value.length() - unit.getKey().length()).trim()) * unit.getValue();
            }
        }
        return 0;
    }

    private float parseNumber(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Nothing or a malformed value: the image states no size we can read
            return 0;
        }
    }

    private float getMaxWidth(Element img, Map<Integer, Integer> columnWidths, float columnCountBasedWidth, float paramsBasedWidth) {
        final float maxWidth;
        int column = getImageColumn(img);
        int colspan = getImageColspan(img);

        if (columnWidths.containsKey(column)) {
            // If column widths were successfully obtained from pre-rendering - take it. Most precise approach.
            // Sum up widths of all spanned columns if colspan > 1
            int totalWidth = 0;
            for (int i = 0; i < colspan; i++) {
                totalWidth += columnWidths.getOrDefault(column + i, 0);
            }
            maxWidth = totalWidth;
        } else {
            // ... otherwise calculate columns width based on columns count - page width equally divided on columns count, as a fallback. Not ideal but works pretty well for most cases.
            maxWidth = columnCountBasedWidth != -1 ? columnCountBasedWidth : paramsBasedWidth;
        }
        return maxWidth;
    }

    private void adjustImageStyle(Element img, float maxWidth, float statedWidth) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(style);

        img.removeAttr(CssProp.WIDTH);
        img.removeAttr(CssProp.HEIGHT);

        // The column shrinks the width, and a height stated for the width the image no longer has would
        // distort it. The image keeps its own ratio instead: a document states such a height by a drag of a
        // handle in the editor, and honouring one smears an image across a page of its own.
        CssUtils.removeProperty(cssStyles, CssProp.HEIGHT);

        if (statedWidth > 0) {
            // The column is what limits the image, it never enlarges one: an image which states no width keeps none
            float adjustedWidth = Math.min(statedWidth, maxWidth);
            CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, ((int) adjustedWidth) + Measure.PX);
        }
        // For svg-images in tables width attribute is not enough, WeasyPrint needs max-width as well
        CssUtils.setPropertyValue(cssStyles, CssProp.MAX_WIDTH, ((int) maxWidth) + Measure.PX);

        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
    }

    private int getImageColumn(Element img) {
        Element columnElement = img.closest(TD_TH_SELECTOR);
        if (columnElement != null) {
            int column = 0;
            Element prevSibling = columnElement.previousElementSibling();
            while (prevSibling != null) {
                column += getColspan(prevSibling);
                prevSibling = prevSibling.previousElementSibling();
            }
            return column;
        } else {
            return -1;
        }
    }

    @VisibleForTesting
    int getImageWidthBasedOnColumnsCount(Element img) {
        Element row = img.closest(HtmlTag.TR);
        if (row != null) {
            int columnsCount = columnsCount(row);
            if (columnsCount > 0) {
                return PaperSizeUtils.getMaxWidth(conversionParams) / columnsCount;
            }
        }
        return -1;
    }

    @VisibleForTesting
    int columnsCount(Element row) {
        int count = 0;
        for (Element cell : row.select(TD_TH_SELECTOR)) {
            count += getColspan(cell);
        }
        return count;
    }

    private int getImageColspan(Element img) {
        Element columnElement = img.closest(TD_TH_SELECTOR);
        if (columnElement != null) {
            return getColspan(columnElement);
        }
        return 1;
    }

    private int getColspan(Element element) {
        String colspanAttr = element.attr("colspan");
        if (!colspanAttr.isEmpty()) {
            try {
                return Integer.parseInt(colspanAttr);
            } catch (NumberFormatException e) {
                // Wrong value in colspan attribute. We shouldn't do anything in this case as it won't be handled properly in final rendering as well, just ignore
            }
        }
        // When colspan is not specified or malformed
        return 1;
    }
}
