package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PageLayout;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.property.ECSSProperty;
import com.helger.css.propertyvalue.CCSSValue;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImageSizeAdjuster extends AbstractAdjuster {

    /** What the CSS of the export says of the page: the height it leaves its content. */
    private final @NotNull PageLayout pageLayout;

    public ImageSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this(document, conversionParams, PageLayout.NONE);
    }

    public ImageSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams, @NotNull PageLayout pageLayout) {
        super(document, conversionParams);
        this.pageLayout = pageLayout;
    }

    @Override
    @SuppressWarnings("java:S9395") // sizes in pixels, which a float holds exactly; java:S1905 reports the explicit cast
    public void execute() {
        float maxWidth = PaperSizeUtils.getMaxWidth(conversionParams);
        float maxHeight = pageLayout.heightForAnImage(conversionParams);

        Elements images = document.select("img[style]");

        for (Element img : images) {
            adjustImageSize(img, maxWidth, maxHeight);
        }
    }

    private void adjustImageSize(@NotNull Element img, float maxWidth, float maxHeight) {
        String style = img.attr(HtmlTagAttr.STYLE);
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations(style);

        // As a fallback we always restrict max height for the cases when image doesn't have any explicit width/height attributes
        // A limit smaller than the page is kept: a table sets one, leaving room for the header it repeats,
        // and a document may state one of its own. Anything larger than the page is the page. A limit which
        // cannot be read as a length, a percentage of a cell for one, is left as the document wrote it.
        String statedLimit = CssUtils.getPropertyValue(cssStyles, ECSSProperty.MAX_HEIGHT);
        float limitInPx = extractDimension(cssStyles, ECSSProperty.MAX_HEIGHT);
        boolean unreadableLimit = !statedLimit.isEmpty() && limitInPx == 0;
        if (!unreadableLimit && (statedLimit.isEmpty() || limitInPx > maxHeight)) {
            CssUtils.setPropertyValue(cssStyles, ECSSProperty.MAX_HEIGHT, (int) maxHeight + Measure.PX);
        }
        if (!statesHeight(img, cssStyles) && CssUtils.getPropertyValue(cssStyles, ECSSProperty.OBJECT_FIT).isEmpty()) {
            // That clamp shortens the height alone, the width being the one given: the image is then stretched.
            // Only the drawing follows this property, so an image which states its own height keeps what it states,
            // and so does an image which states how it is to be drawn.
            CssUtils.setPropertyValue(cssStyles, ECSSProperty.OBJECT_FIT, CCSSValue.CONTAIN);
        }
        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());

        float cssWidth = extractDimension(cssStyles, ECSSProperty.WIDTH);
        float cssMaxWidth = extractDimension(cssStyles, ECSSProperty.MAX_WIDTH);
        float cssHeight = extractDimension(cssStyles, ECSSProperty.HEIGHT);

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
            CssUtils.setPropertyValue(cssStyles, ECSSProperty.WIDTH, (int) adjustedWidth + Measure.PX);
        }
        if (adjustedMaxWidth > 0) {
            CssUtils.setPropertyValue(cssStyles, ECSSProperty.MAX_WIDTH, (int) adjustedMaxWidth + Measure.PX);
        }
        if (adjustedHeight > 0) {
            CssUtils.setPropertyValue(cssStyles, ECSSProperty.HEIGHT, (int) adjustedHeight + Measure.PX);
        }

        img.attr(HtmlTagAttr.STYLE, cssStyles.getAsCSSString());
    }

    private boolean statesHeight(@NotNull Element img, CSSDeclarationList cssStyles) {
        String height = CssUtils.getPropertyValue(cssStyles, ECSSProperty.HEIGHT);
        // "auto" states no height: the height still follows the width, so the clamp still stretches the image
        return (!height.isEmpty() && !CCSSValue.AUTO.equalsIgnoreCase(height)) || img.hasAttr(HtmlTagAttr.HEIGHT);
    }

    /**
     * The length in pixels, where the unit it is stated in says how long it is on its own. A length stated in
     * a unit which depends on the element it sits on, such as a percentage, reads as none.
     */
    private float extractDimension(CSSDeclarationList cssStyles, ECSSProperty property) {
        String value = CssUtils.getPropertyValue(cssStyles, property);

        for (Map.Entry<String, Float> unit : Measure.ABSOLUTE_UNITS_IN_PX.entrySet()) {
            if (value.endsWith(unit.getKey())) {
                try {
                    return Float.parseFloat(value.substring(0, value.length() - unit.getKey().length()).trim()) * unit.getValue();
                } catch (NumberFormatException e) {
                    return 0; // A length which is no number says nothing about the size of the image
                }
            }
        }
        return 0;
    }

    private float divide(float value, float divisor) {
        return divisor != 0 ? value / divisor : value;
    }
}
