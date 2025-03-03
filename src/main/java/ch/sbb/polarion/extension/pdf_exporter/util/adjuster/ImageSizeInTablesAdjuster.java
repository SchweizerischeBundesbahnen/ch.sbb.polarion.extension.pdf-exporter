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

import java.util.Optional;

public class ImageSizeInTablesAdjuster extends AbstractAdjuster {

    public ImageSizeInTablesAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        Elements tables = document.select("table");

        for (Element table : tables) {
            Elements images = table.select("img");

            for (Element img : images) {
                float cssWidth = extractWidth(img, CssProp.WIDTH);
                float cssMaxWidth = extractWidth(img, CssProp.MAX_WIDTH);

                float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(img);
                float paramsBasedWidth = PaperSizeUtils.getMaxWidthInTables(conversionParams);

                float maxWidth = (columnCountBasedWidth != -1 && columnCountBasedWidth < paramsBasedWidth)
                        ? columnCountBasedWidth
                        : paramsBasedWidth;

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

        cssStyle.removeProperty(CssProp.MAX_WIDTH); //it seems that max-width doesn't work in WeasyPrint
        cssStyle.removeProperty(CssProp.HEIGHT); //remove height completely in order to keep image ratio

        cssStyle.setProperty(CssProp.WIDTH, ((int) maxWidth) + Measure.PX, "");
        img.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
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
