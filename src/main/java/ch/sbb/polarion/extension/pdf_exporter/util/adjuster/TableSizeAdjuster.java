package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

public class TableSizeAdjuster extends AbstractAdjuster {

    public TableSizeAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        super(document, conversionParams);
    }

    @Override
    public void execute() {

    }
}
