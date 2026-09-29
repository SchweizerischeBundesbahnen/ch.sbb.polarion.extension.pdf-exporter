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

import java.util.Map;
public class ImageSizeInTablesAdjuster extends AbstractAdjuster {

    private static final String TD_TH_SELECTOR = String.format("%s, %s", HtmlTag.TD, HtmlTag.TH);

    /** What a cell adds around the image it holds: the padding Polarion writes and the border of the cell. */
    private static final int CELL_CHROME_PX = 16;

    /** However tall a header grows, an image is still worth seeing. */
    private static final int MIN_IMAGE_HEIGHT_PX = 100;

    /**
     * The step the height a header takes is rounded up to. The height is measured by laying the table out,
     * and a font renders a pixel taller on one machine than on another; a step keeps the same document the
     * same size wherever it is exported.
     */
    private static final int HEADER_HEIGHT_STEP_PX = 25;

    public ImageSizeInTablesAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        Elements tables = document.select(HtmlTag.TABLE);

        for (Element table : tables) {
            Elements images = table.select(HtmlTag.IMG);
            if (images.isEmpty()) {
                continue;
            }

            // Pre-render table and get rendered column widths proportionally adjusted to page width
            TableAnalyzer.TableMetrics metrics = TableAnalyzer.analyze(table, PaperSizeUtils.getMaxWidth(conversionParams));
            Map<Integer, Integer> columnWidths = metrics.columnWidths();

            for (Element img : images) {
                limitHeight(img, metrics.headerHeight());

                float cssWidth = extractWidth(img, CssProp.WIDTH);
                float cssMaxWidth = extractWidth(img, CssProp.MAX_WIDTH);

                float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(img);
                float paramsBasedWidth = PaperSizeUtils.getMaxWidthInTables(conversionParams);

                float maxWidth = getMaxWidth(img, columnWidths, columnCountBasedWidth, paramsBasedWidth);

                if (cssWidth > maxWidth || cssMaxWidth > maxWidth) {
                    adjustImageStyle(img, maxWidth, cssWidth);
                }
            }
        }
    }

    /**
     * A row which fills the page to its last pixel cannot carry the header of its table: the header is then
     * left on the page before, above nothing, or dropped altogether. The image gives that height up.
     */
    private void limitHeight(Element img, int headerHeight) {
        int roundedHeader = (int) (Math.ceil((double) headerHeight / HEADER_HEIGHT_STEP_PX) * HEADER_HEIGHT_STEP_PX);
        int allowedHeight = Math.max(PaperSizeUtils.getMaxHeight(conversionParams) - roundedHeader - CELL_CHROME_PX, MIN_IMAGE_HEIGHT_PX);

        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(img.attr(HtmlTagAttr.STYLE));
        float statedHeight = extractPixels(CssUtils.getPropertyValue(cssStyles, CssProp.MAX_HEIGHT));
        if (statedHeight > 0 && statedHeight <= allowedHeight) {
            // The image asks for less than the page leaves it, and what it asks for is what it keeps
            return;
        }

        CssUtils.setPropertyValue(cssStyles, CssProp.MAX_HEIGHT, allowedHeight + Measure.PX);
        if (statedSize(img, cssStyles, CssProp.HEIGHT) > allowedHeight && CssUtils.getPropertyValue(cssStyles, CssProp.OBJECT_FIT).isEmpty()) {
            // The limit cuts into the height the image states, and a height cut alone squashes the drawing
            CssUtils.setPropertyValue(cssStyles, CssProp.OBJECT_FIT, CssProp.OBJECT_FIT_CONTAIN_VALUE);
        }
        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
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

    private float extractPixels(String value) {
        if (value.endsWith(Measure.EX)) {
            return parseNumber(value.replace(Measure.EX, "")) * Measure.EX_TO_PX_RATIO;
        } else if (value.endsWith(Measure.PX)) {
            return parseNumber(value.replace(Measure.PX, ""));
        } else {
            return 0;
        }
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
