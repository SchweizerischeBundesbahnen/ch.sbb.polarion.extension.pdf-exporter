package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.css.CSSStyleDeclaration;

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
        CSSStyleDeclaration cssStyle = parseCss(style);

        // As a fallback we always restrict max height for the cases when image doesn't have any explicit width/height attributes
        cssStyle.setProperty(CssProp.MAX_HEIGHT, (int) maxHeight + Measure.PX, "");
        img.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());

        float cssWidth = extractDimension(cssStyle, CssProp.WIDTH);
        float cssMaxWidth = extractDimension(cssStyle, CssProp.MAX_WIDTH);
        float cssHeight = extractDimension(cssStyle, CssProp.HEIGHT);

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
            cssStyle.setProperty(CssProp.WIDTH, (int) adjustedWidth + Measure.PX, "");
        }
        if (adjustedMaxWidth > 0) {
            cssStyle.setProperty(CssProp.MAX_WIDTH, (int) adjustedMaxWidth + Measure.PX, "");
        }
        if (adjustedHeight > 0) {
            cssStyle.setProperty(CssProp.HEIGHT, (int) adjustedHeight + Measure.PX, "");
        }

        img.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
    }

    private float extractDimension(CSSStyleDeclaration cssStyle, String property) {
        String value = Optional.ofNullable(cssStyle.getPropertyValue(property)).orElse("").trim();

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
