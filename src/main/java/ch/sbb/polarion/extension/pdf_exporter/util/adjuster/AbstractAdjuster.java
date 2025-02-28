package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.CSSOMParser;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.io.StringReader;

public abstract class AbstractAdjuster {

    protected final @NotNull Document document;
    protected final @NotNull ConversionParams conversionParams;
    protected final @NotNull CSSOMParser parser;

    public AbstractAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this.document = document;
        this.conversionParams = conversionParams;
        this.parser = new CSSOMParser();
    }

    public abstract void execute();


    protected CSSStyleDeclaration parseCss(String style) {
        try {
            return parser.parseStyleDeclaration(new InputSource(new StringReader(style)));
        } catch (Exception e) {
            return new CSSStyleDeclarationImpl();
        }
    }
}
