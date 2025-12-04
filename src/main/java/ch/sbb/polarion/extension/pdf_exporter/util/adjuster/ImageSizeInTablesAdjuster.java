package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.util.Map;
import java.util.Optional;

public class ImageSizeInTablesAdjuster extends AbstractAdjuster {

    private static final String TD_TH_SELECTOR = String.format("%s, %s", HtmlTag.TD, HtmlTag.TH);

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
            Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(conversionParams));

            for (Element img : images) {
                float cssWidth = extractWidth(img, CssProp.WIDTH);
                float cssMaxWidth = extractWidth(img, CssProp.MAX_WIDTH);

                float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(img);
                float paramsBasedWidth = PaperSizeUtils.getMaxWidthInTables(conversionParams);

                float maxWidth = getMaxWidth(img, columnWidths, columnCountBasedWidth, paramsBasedWidth);

                if (cssWidth > maxWidth || cssMaxWidth > maxWidth) {
                    adjustImageStyle(img, maxWidth);
                }
            }
        }
    }

    private float extractWidth(Element img, String property) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString(style)).orElse(new CSSDeclarationList());

        String value = CssUtils.getPropertyValue(cssStyles, property);
        if (value.isEmpty()) {
            return 0;
        }

        if (value.equals("auto")) {
            return Float.MAX_VALUE;
        }

        if (value.endsWith(Measure.EX)) {
            return Float.parseFloat(value.replace(Measure.EX, "")) * Measure.EX_TO_PX_RATIO;
        } else if (value.endsWith(Measure.PX)) {
            return Float.parseFloat(value.replace(Measure.PX, ""));
        } else if (value.endsWith(Measure.PERCENT)) {
            return PaperSizeUtils.getMaxWidthInTables(conversionParams) * Float.parseFloat(value.replace(Measure.PERCENT, ""));
        } else {
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

    private void adjustImageStyle(Element img, float maxWidth) {
        img.removeAttr(CssProp.WIDTH);
        img.removeAttr(CssProp.HEIGHT);

        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString(style)).orElse(new CSSDeclarationList());

        CssUtils.removeProperty(cssStyles, CssProp.HEIGHT); //remove height completely in order to keep image ratio

        CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, ((int) maxWidth) + Measure.PX);
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
