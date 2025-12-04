package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

public class TableSizeAdjuster extends AbstractAdjuster {

    public TableSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        float maxWidth = PaperSizeUtils.getMaxWidth(conversionParams);

        Elements tables = document.select("table[style]");
        for (Element table : tables) {
            CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString(table.attr(HtmlTagAttr.STYLE))).orElse(new CSSDeclarationList());

            float width = extractDimension(CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));

            if (width > maxWidth) {
                CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, "100%");
            }

            table.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
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
            return PaperSizeUtils.getMaxWidth(conversionParams) * Float.parseFloat(value.replace(Measure.PERCENT, "").trim());
        }
        return 0;
    }

}
