package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.constants.CssStyle;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.constants.PaperSizeConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImageSizeInTablesAdjuster extends AbstractAdjuster {
    public static final String TABLE_ROW_OPEN_TAG = "<tr";
    public static final String TABLE_ROW_END_TAG = "</tr>";
    public static final String TABLE_COLUMN_OPEN_TAG = "<td";

    public ImageSizeInTablesAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        Elements tables = document.select("table");

        for (Element table : tables) {
            Elements images = table.select("img");

            for (Element img : images) {
                String style = img.attr(HtmlTagAttr.STYLE);
                float width = extractWidth(style);

                float columnCountBasedWidth = getImageWidthBasedOnColumnsCount(table.outerHtml(), img.outerHtml());
                float paramsBasedWidth = PaperSizeConstants.getMaxWidthInTables(conversionParams);

                float maxWidth = (columnCountBasedWidth != -1 && columnCountBasedWidth < paramsBasedWidth)
                        ? columnCountBasedWidth
                        : paramsBasedWidth;

                if (width > maxWidth) {
                    adjustImageStyle(img, maxWidth);
                }
            }
        }
    }

    private float extractWidth(String style) {
        if (style.contains(CssStyle.WIDTH)) {
            String widthValue = RegexMatcher.get("width:\s*([\\d.]+(px|ex)|auto);", RegexMatcher.DOTALL | RegexMatcher.CASE_INSENSITIVE)
                    .findFirst(style, regexEngine -> regexEngine.group(1))
                    .orElse("");

            if (widthValue.equals("auto")) {
                return Float.MAX_VALUE;
            }

            if (widthValue.endsWith(Measure.EX)) {
                return Float.parseFloat(widthValue.replace(Measure.EX, "")) * Measure.EX_TO_PX_RATIO;
            } else {
                return Float.parseFloat(widthValue.replace(Measure.PX, ""));
            }
        }
        return 0;
    }

    private void adjustImageStyle(Element img, float maxWidth) {
        img.removeAttr(CssStyle.WIDTH);
        img.removeAttr(CssStyle.HEIGHT);

        String style = img.attr(HtmlTagAttr.STYLE);
        style = style.replaceAll("max-width:\s*([\\d.]+(px|ex)|auto);", ""); //it seems that max-width doesn't work in WeasyPrint
        style = style.replaceAll("width:\s*([\\d.]+(px|ex)|auto);", ""); //remove width too, we will add it later
        style = style.replaceAll("height:\s*[\\d.]+(px|ex);", ""); //remove height completely in order to keep image ratio
        String styleWithWidth = "width: " + ((int) maxWidth) + "px; " + style;
        img.attr(HtmlTagAttr.STYLE, styleWithWidth);
    }

    @VisibleForTesting
    int getImageWidthBasedOnColumnsCount(String table, String imgTag) {
        int imgPosition = table.indexOf(imgTag);
        int trStartPosition = table.substring(0, imgPosition).lastIndexOf(TABLE_ROW_OPEN_TAG);
        int trEndPosition = table.indexOf(TABLE_ROW_END_TAG, imgPosition);
        if (trStartPosition != -1 && trEndPosition != -1) {
            int columnsCount = columnsCount(table.substring(trStartPosition, trEndPosition));
            if (columnsCount > 0) {
                return PaperSizeConstants.getMaxWidth(conversionParams) / columnsCount;
            }
        }
        return -1;
    }

    @VisibleForTesting
    int columnsCount(String string) {
        return (string.length() - string.replace(TABLE_COLUMN_OPEN_TAG, "").length()) / TABLE_COLUMN_OPEN_TAG.length();
    }

}
