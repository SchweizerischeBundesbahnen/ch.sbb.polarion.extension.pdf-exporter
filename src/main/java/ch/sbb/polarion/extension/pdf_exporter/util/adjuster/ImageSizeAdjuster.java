package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

public class ImageSizeAdjuster extends AbstractAdjuster {

    public ImageSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        float maxWidth = PaperSizeUtils.getMaxWidth(conversionParams);
        float maxHeight = PaperSizeUtils.getMaxHeight(conversionParams);

        Elements images = document.select("img[style]");

        for (Element img : images) {
            adjustImageSize(img, maxWidth, maxHeight);
        }
    }

    private void adjustImageSize(@NotNull Element img, float maxWidth, float maxHeight) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = Optional.ofNullable(CSSReaderDeclarationList.readFromString(style)).orElse(new CSSDeclarationList());

        // As a fallback we always restrict max height for the cases when image doesn't have any explicit width/height attributes
        CssUtils.setPropertyValue(cssStyles, CssProp.MAX_HEIGHT, (int) maxHeight + Measure.PX);
        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());

        float cssWidth = extractDimension(cssStyles, CssProp.WIDTH);
        float cssMaxWidth = extractDimension(cssStyles, CssProp.MAX_WIDTH);
        float cssHeight = extractDimension(cssStyles, CssProp.HEIGHT);

        float widthExceedingRatio = cssWidth / maxWidth;
        float maxWidthExceedingRatio = cssMaxWidth / maxWidth;
        float heightExceedingRatio = cssHeight / maxHeight;

        if (widthExceedingRatio <= 1 && heightExceedingRatio <= 1 && maxWidthExceedingRatio <= 1) {
            return;
        }

        float adjustedWidth = 0;
        float adjustedMaxWidth = 0;
        float adjustedHeight = 0;

        if (widthExceedingRatio > heightExceedingRatio) {
            adjustedWidth = divide(cssWidth, widthExceedingRatio);
            adjustedHeight = divide(cssHeight, widthExceedingRatio);
        } else if (maxWidthExceedingRatio > heightExceedingRatio) {
            adjustedMaxWidth = divide(cssMaxWidth, maxWidthExceedingRatio);
            adjustedHeight = divide(cssHeight, maxWidthExceedingRatio);
        } else {
            adjustedMaxWidth = divide(cssMaxWidth, heightExceedingRatio);
            adjustedWidth = divide(cssWidth, heightExceedingRatio);
            adjustedHeight = divide(cssHeight, heightExceedingRatio);
        }

        if (adjustedWidth > 0) {
            CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, (int) adjustedWidth + Measure.PX);
        }
        if (adjustedMaxWidth > 0) {
            CssUtils.setPropertyValue(cssStyles, CssProp.MAX_WIDTH, (int) adjustedMaxWidth + Measure.PX);
        }
        if (adjustedHeight > 0) {
            CssUtils.setPropertyValue(cssStyles, CssProp.HEIGHT, (int) adjustedHeight + Measure.PX);
        }

        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
    }

    private float extractDimension(CSSDeclarationList cssStyles, String property) {
        String value = CssUtils.getPropertyValue(cssStyles, property);

        if (value.isEmpty()) {
            return 0;
        }

        if (value.endsWith(Measure.EX)) {
            return Float.parseFloat(value.replace(Measure.EX, "")) * Measure.EX_TO_PX_RATIO;
        } else if (value.endsWith(Measure.PX)) {
            return Float.parseFloat(value.replace(Measure.PX, ""));
        } else if (value.endsWith(Measure.PERCENT)) {
            return 0;
        } else {
            return 0;
        }
    }

    private float divide(float value, float divisor) {
        return divisor != 0 ? value / divisor : value;
    }
}
