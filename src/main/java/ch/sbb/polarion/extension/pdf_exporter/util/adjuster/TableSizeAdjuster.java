package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssStyle;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.constants.PaperSizeConstants;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.css.CSSStyleDeclaration;

public class TableSizeAdjuster extends AbstractAdjuster {

    public TableSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        float maxWidth = PaperSizeConstants.getMaxWidth(conversionParams);

        Elements tables = document.select("table[style]");
        for (Element table : tables) {
            CSSStyleDeclaration cssStyle = parseCss(table.attr(HtmlTagAttr.STYLE));

            float width = extractDimension(cssStyle.getPropertyValue(CssStyle.WIDTH));

            if (width > maxWidth) {
                cssStyle.setProperty(CssStyle.WIDTH, "100%", "");
            }

            table.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
        }
    }

    private float extractDimension(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        value = value.trim();
        if (value.endsWith(Measure.PX)) {
            return Float.parseFloat(value.replace(Measure.PX, "").trim());
        } else if (value.endsWith(Measure.PERCENT)) {
            return PaperSizeConstants.getMaxWidth(conversionParams) * Float.parseFloat(value.replace(Measure.PERCENT, "").trim());
        }
        return 0;
    }

}
