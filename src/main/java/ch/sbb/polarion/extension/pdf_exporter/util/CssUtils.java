package ch.sbb.polarion.extension.pdf_exporter.util;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.CSSOMParser;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.io.StringReader;

@UtilityClass
public class CssUtils {

    public CSSStyleDeclaration parseCss(@NotNull CSSOMParser parser, @NotNull String style) {
        try {
            return parser.parseStyleDeclaration(new InputSource(new StringReader(style)));
        } catch (Exception e) {
            return new CSSStyleDeclarationImpl();
        }
    }
}
