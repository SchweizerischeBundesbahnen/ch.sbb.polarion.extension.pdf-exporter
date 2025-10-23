package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.CssUtils;
import com.steadystate.css.parser.CSSOMParser;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.w3c.dom.css.CSSStyleDeclaration;

public abstract class AbstractAdjuster {
    protected final @NotNull Document document;
    protected final @NotNull ConversionParams conversionParams;
    private final @NotNull CSSOMParser parser = new CSSOMParser();

    protected AbstractAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this.document = document;
        this.conversionParams = conversionParams;
    }

    public abstract void execute();

    protected CSSStyleDeclaration parseCss(String style) {
        return CssUtils.parseCss(parser, style);
    }
}
