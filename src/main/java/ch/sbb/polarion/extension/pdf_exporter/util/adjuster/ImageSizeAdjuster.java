package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeConstants;
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

        // get max width and height depending on page orientation
        float maxWidth = conversionParams.getOrientation() == Orientation.PORTRAIT
                ? PaperSizeConstants.MAX_PORTRAIT_WIDTHS.get(conversionParams.getPaperSize())
                : PaperSizeConstants.MAX_LANDSCAPE_WIDTHS.get(conversionParams.getPaperSize());
        float maxHeight = conversionParams.getOrientation() == Orientation.PORTRAIT
                ? PaperSizeConstants.MAX_PORTRAIT_HEIGHTS.get(conversionParams.getPaperSize())
                : PaperSizeConstants.MAX_LANDSCAPE_HEIGHTS.get(conversionParams.getPaperSize());

        // We are looking here for images which widths and heights are explicitly specified.
        // Then we check if width exceeds limit we override it by value "100%"
        Elements images = document.select("img[style]"); // getting <img> with style attribute

        for (Element img : images) {
            adjustImageSize(img, maxWidth, maxHeight);
        }
    }

    private void adjustImageSize(@NotNull Element img, float maxWidth, float maxHeight) {
        String style = img.attr("style");

        float width = extractDimension(style, "width");
        float height = extractDimension(style, "height");

        float widthExceedingRatio = width / maxWidth;
        float heightExceedingRatio = height / maxHeight;

        if (widthExceedingRatio <= 1 && heightExceedingRatio <= 1) {
            return;
        }

        final float adjustedWidth;
        final float adjustedHeight;

        if (widthExceedingRatio > heightExceedingRatio) {
            adjustedWidth = width / widthExceedingRatio;
            adjustedHeight = height / widthExceedingRatio;
        } else {
            adjustedWidth = width / heightExceedingRatio;
            adjustedHeight = height / heightExceedingRatio;
        }

        style = style.replaceAll("width:\\s*[^;]+;", String.format("width: %dpx;", (int) adjustedWidth)).trim();
        style = style.replaceAll("height:\\s*[^;]+;", String.format("height: %dpx;", (int) adjustedHeight)).trim();

        img.attr("style", style);
    }

    private float extractDimension(String style, String property) {
        String regex = property + ":\\s*([\\d.]+)(px|ex)?";
        String result = RegexMatcher.get(regex, RegexMatcher.DOTALL | RegexMatcher.CASE_INSENSITIVE)
                .findFirst(style, regexEngine -> {
                    String value = regexEngine.group(1);
                    String unit = Optional.ofNullable(regexEngine.group(2)).orElse("");
                    return value + unit;
                })
                .orElse("0");

        if (result.endsWith("ex")) {
            return Float.parseFloat(result.replace("ex", "")) * PageWidthAdjuster.EX_TO_PX_RATIO;
        } else {
            return Float.parseFloat(result.replace("px", ""));
        }
    }
}
