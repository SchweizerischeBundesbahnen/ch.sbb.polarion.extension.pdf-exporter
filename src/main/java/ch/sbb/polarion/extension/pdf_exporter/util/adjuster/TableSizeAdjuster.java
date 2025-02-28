package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeConstants;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TableSizeAdjuster extends AbstractAdjuster {

    public TableSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {
        float maxWidth = PaperSizeConstants.getMaxWidth(conversionParams);

        Elements tables = document.select("table[style*=width]");
        for (Element table : tables) {
            String style = table.attr("style");
            String[] styles = style.split(";");
            StringBuilder newStyle = new StringBuilder();

            for (String s : styles) {
                if (s.trim().startsWith("width")) {
                    String[] parts = s.split(":");
                    if (parts.length == 2) {
                        String value = parts[1].trim();
                        if (value.endsWith("px")) {
                            float widthParsed = Float.parseFloat(value.replace("px", "").trim());
                            if (widthParsed > maxWidth) {
                                newStyle.append("width: 100%;");
                                continue;
                            }
                        } else if (value.endsWith("%")) {
                            float widthParsed = Float.parseFloat(value.replace("%", "").trim());
                            if (widthParsed > 100) {
                                newStyle.append("width: 100%;");
                                continue;
                            }
                        }
                    }
                }
                newStyle.append(s).append(";");
            }

            table.attr("style", newStyle.toString());
        }
    }
}
