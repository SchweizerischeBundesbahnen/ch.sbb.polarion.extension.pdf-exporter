package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.util.Map;
import java.util.Optional;

public class ImageSizeInTablesAdjuster extends AbstractAdjuster {

    public ImageSizeInTablesAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        Elements tables = document.select("table");

        for (Element table : tables) {
            // Pre-render table and get rendered column widths proportionally adjusted to page width
            Map<Integer, Integer> columnWidths = TableAnalyzer.getColumnWidths(table, PaperSizeUtils.getMaxWidth(conversionParams));

            for (Element img : table.select("img")) {
                float cssWidth = extractWidth(img, CssProp.WIDTH);
                float cssMaxWidth = extractWidth(img, CssProp.MAX_WIDTH);

                float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(img);
                float paramsBasedWidth = PaperSizeUtils.getMaxWidthInTables(conversionParams);

                final float maxWidth;
                int column = getImageColumn(img);
                if (columnWidths.containsKey(column)) {
                    // If column widths were successfully obtained from pre-rendering - take it. Most precise approach.
                    maxWidth = columnWidths.get(column);
                } else {
                    // ... otherwise calculate columns width based on columns count - page width equally divided on columns count, as a fallback. Not ideal but works pretty well for most cases.
                    maxWidth = columnCountBasedWidth != -1 ? columnCountBasedWidth : paramsBasedWidth;
                }

                if (cssWidth > maxWidth || cssMaxWidth > maxWidth) {
                    adjustImageStyle(img, maxWidth);
                }
            }
        }
    }

    private float extractWidth(Element img, String property) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSStyleDeclaration cssStyle = parseCss(style);

        String value = Optional.ofNullable(cssStyle.getPropertyValue(property)).orElse("").trim();

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

    private void adjustImageStyle(Element img, float maxWidth) {
        img.removeAttr(CssProp.WIDTH);
        img.removeAttr(CssProp.HEIGHT);

        String style = img.attr(HtmlTagAttr.STYLE);
        CSSStyleDeclaration cssStyle = parseCss(style);

        cssStyle.removeProperty(CssProp.HEIGHT); //remove height completely in order to keep image ratio

        cssStyle.setProperty(CssProp.WIDTH, ((int) maxWidth) + Measure.PX, "");
        // For svg-images in tables width attribute is not enough, WeasyPrint needs max-width as well
        cssStyle.setProperty(CssProp.MAX_WIDTH, ((int) maxWidth) + Measure.PX, "");

        img.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
    }

    private int getImageColumn(Element img) {
        Element columnElement = img.closest("td");
        if (columnElement != null) {
            int column = 0;
            Element prevSibling = columnElement.previousElementSibling();
            while (prevSibling != null) {
                column++;
                prevSibling = prevSibling.previousElementSibling();
            }
            return column;
        } else {
            return -1;
        }
    }

    @VisibleForTesting
    int getImageWidthBasedOnColumnsCount(Element img) {
        Element row = img.closest("tr");
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
        for (Element cell : row.select("td, th")) {
            String colspanAttr = cell.attr("colspan");
            int colspan = colspanAttr.isEmpty() ? 1 : Integer.parseInt(colspanAttr);
            count += colspan;
        }
        return count;
    }
}
