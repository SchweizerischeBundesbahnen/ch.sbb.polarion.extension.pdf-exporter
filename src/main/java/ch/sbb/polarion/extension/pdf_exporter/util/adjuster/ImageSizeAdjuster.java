package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeConstants;
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
        float maxWidth = PaperSizeConstants.getMaxWidth(conversionParams);
        float maxHeight = PaperSizeConstants.getMaxHeight(conversionParams);

        Elements images = document.select("img[style]");

        for (Element img : images) {
            adjustImageSize(img, maxWidth, maxHeight);
        }
    }

    private void adjustImageSize(@NotNull Element img, float maxWidth, float maxHeight) {
        String style = img.attr("style");
        CSSStyleDeclaration cssStyle = parseCss(style);

        float cssWidth = extractDimension(cssStyle, "width");
        float cssMaxWidth = extractDimension(cssStyle, "max-width");
        float cssHeight = extractDimension(cssStyle, "height");

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
            cssStyle.setProperty("width", (int) adjustedWidth + "px", "");
        }
        if (adjustedMaxWidth > 0) {
            cssStyle.setProperty("max-width", (int) adjustedMaxWidth + "px", "");
        }
        if (adjustedHeight > 0) {
            cssStyle.setProperty("height", (int) adjustedHeight + "px", "");
        }

        img.attr("style", cssStyle.getCssText());
    }

    private float extractDimension(CSSStyleDeclaration cssStyle, String property) {
        String value = Optional.ofNullable(cssStyle.getPropertyValue(property)).orElse("").trim();

        if (value.isEmpty()) {
            return 0;
        }

        if (value.endsWith("ex")) {
            return Float.parseFloat(value.replace("ex", "")) * HtmlProcessor.EX_TO_PX_RATIO;
        } else if (value.endsWith("px")) {
            return Float.parseFloat(value.replace("px", ""));
        } else if (value.endsWith("%")) {
            return 0;
        } else {
            return 0;
        }
    }

    private float divide(float value, float divisor) {
        return divisor != 0 ? value / divisor : value;
    }
}
