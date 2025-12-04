package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.decl.CSSExpression;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@UtilityClass
public class CssUtils {

    @NotNull
    public String getPropertyValue(@NotNull CSSDeclarationList cssStyles, @NotNull String propertyName) {
        for (CSSDeclaration decl : cssStyles.getAllDeclarations()) {
            if (decl.getProperty().equalsIgnoreCase(propertyName)) {
                return Optional.ofNullable(stripQuotes(decl.getExpressionAsCSSString())).orElse("");
            }
        }
        return "";
    }

    @Nullable
    private String stripQuotes(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        // Remove surrounding quotes (single or double)
        if (trimmed.length() > 1 && (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        } else {
            return trimmed;
        }
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
