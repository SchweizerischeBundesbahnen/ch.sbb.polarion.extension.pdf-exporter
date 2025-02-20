package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import java.util.Optional;

import static ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor.*;

public class PageWidthAdjuster {

    private static final float EX_TO_PX_RATIO = 6.5F;

    private final @NotNull Document document;
    private final @NotNull ConversionParams conversionParams;

    public PageWidthAdjuster(@NotNull String html, @NotNull ConversionParams conversionParams) {
        this.document = Jsoup.parse(html);
        this.document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.base)
                .prettyPrint(false);
        this.conversionParams = conversionParams;
    }

    public void adjustImageSize() {
        // get max width and height depending on page orientation
        float maxWidth = conversionParams.getOrientation() == Orientation.PORTRAIT ? MAX_PORTRAIT_WIDTHS.get(conversionParams.getPaperSize()) : MAX_LANDSCAPE_WIDTHS.get(conversionParams.getPaperSize());
        float maxHeight = conversionParams.getOrientation() == Orientation.PORTRAIT ? MAX_PORTRAIT_HEIGHTS.get(conversionParams.getPaperSize()) : MAX_LANDSCAPE_HEIGHTS.get(conversionParams.getPaperSize());

        // We are looking here for images which widths and heights are explicitly specified.
        // Then we check if width exceeds limit we override it by value "100%"
        Elements images = document.select("img[style]"); // getting <img> with style attribute

        for (Element img : images) {
            adjustImageSize(img, maxWidth, maxHeight);
        }
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
            return Float.parseFloat(result.replace("ex", "")) * EX_TO_PX_RATIO;
        } else {
            return Float.parseFloat(result.replace("px", ""));
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

    public @NotNull String toHTML() {
        String html = document.body().html();
        // after processing with Jsoup we need to replace $ with &dollar;
        // because of regular expressions, as it has special meaning there
        html = html.replace("$", "&dollar;");
        return html;
    }
}
