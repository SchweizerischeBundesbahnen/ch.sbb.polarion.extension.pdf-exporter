package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.decl.CSSExpression;
import com.helger.css.reader.CSSReaderDeclarationList;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.CSSParseError;
import com.helger.css.reader.errorhandler.CollectingCSSParseErrorHandler;
import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class CssUtils {

    private static final int MAX_STYLE_LOG_LENGTH = 200;

    private static Logger logger = Logger.getLogger(CssUtils.class);

    @org.jetbrains.annotations.VisibleForTesting
    public static void setLogger(Logger testLogger) {
        logger = testLogger;
    }

    @NotNull
    public CSSDeclarationList parseDeclarations(@NotNull String styleAttributeValue) {
        CollectingCSSParseErrorHandler errorHandler = new CollectingCSSParseErrorHandler();
        CSSReaderSettings settings = new CSSReaderSettings().setCustomErrorHandler(errorHandler);
        CSSDeclarationList result = CSSReaderDeclarationList.readFromString(styleAttributeValue, settings);
        if (errorHandler.hasParseErrors()) {
            String stylePreview = truncateForLog(styleAttributeValue);
            for (CSSParseError error : errorHandler.getAllParseErrors()) {
                logger.warn("Failed to parse CSS '" + stylePreview + "': " + error.getErrorMessage());
            }
        }
        return result != null ? result : new CSSDeclarationList();
    }

    private String truncateForLog(@NotNull String value) {
        if (value.length() > MAX_STYLE_LOG_LENGTH) {
            return value.substring(0, MAX_STYLE_LOG_LENGTH) + "... (truncated, total length=" + value.length() + ")";
        }
        return value;
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
