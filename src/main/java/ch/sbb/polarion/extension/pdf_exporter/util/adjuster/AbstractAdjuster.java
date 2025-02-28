package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

public abstract class AbstractAdjuster {

    protected final @NotNull Document document;
    protected final @NotNull ConversionParams conversionParams;

    public AbstractAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this.document = document;
        this.conversionParams = conversionParams;
    }

    public abstract void execute();

}
