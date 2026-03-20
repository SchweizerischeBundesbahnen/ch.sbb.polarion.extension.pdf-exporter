package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.decl.CSSExpression;
import com.helger.css.reader.CSSReaderDeclarationList;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.CSSParseError;
import com.helger.css.reader.errorhandler.CollectingCSSParseErrorHandler;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

@UtilityClass
public class CssUtils {

    private final Logger logger = Logger.getLogger(CssUtils.class.getName());

    @NotNull
    public CSSDeclarationList parseDeclarations(@NotNull String styleAttributeValue) {
        CollectingCSSParseErrorHandler errorHandler = new CollectingCSSParseErrorHandler();
        CSSReaderSettings settings = new CSSReaderSettings().setCustomErrorHandler(errorHandler);
        CSSDeclarationList result = CSSReaderDeclarationList.readFromString(styleAttributeValue, settings);
        if (errorHandler.hasParseErrors()) {
            for (CSSParseError error : errorHandler.getAllParseErrors()) {
                logger.warning("Failed to parse CSS '" + styleAttributeValue + "': " + error.getErrorMessage());
            }
        }
        return result != null ? result : new CSSDeclarationList();
    }

    @NotNull
    public String getPropertyValue(@NotNull CSSDeclarationList cssStyles, @NotNull String propertyName) {
        for (CSSDeclaration decl : cssStyles.getAllDeclarations()) {
            if (decl.getProperty().equalsIgnoreCase(propertyName)) {
                return decl.getExpressionAsCSSString();
            }
        }
        return "";
    }

    public void setPropertyValue(@NotNull CSSDeclarationList cssStyles, @NotNull String propertyName, @NotNull String propertyValue) {
        for (CSSDeclaration declaration : cssStyles.getAllDeclarations()) {
            if (declaration.getProperty().equalsIgnoreCase(propertyName)) {
                // If there's such property declaration - overwrite its value...
                declaration.setExpression(CSSExpression.createSimple(propertyValue));
                return; // ...and stop processing by returning
            }
        }
        // If there's no such property declaration - add it
        cssStyles.add(new CSSDeclaration(propertyName, CSSExpression.createSimple(propertyValue)));
    }

    public void removeProperty(@NotNull CSSDeclarationList cssStyles, @NotNull String propertyName) {
        for (CSSDeclaration declaration : cssStyles.getAllDeclarations()) {
            if (declaration.getProperty().equalsIgnoreCase(propertyName)) {
                cssStyles.removeDeclaration(declaration);
                break;
            }
        }
    }
}
