package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSSelector;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.visit.DefaultCSSVisitor;
import com.helger.css.handler.DoNothingCSSParseExceptionCallback;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import com.helger.css.writer.CSSWriterSettings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds what the CSS of an export says about the height of a table header, so that the measure of a table, which
 * reads inline styles alone, leaves a row under it the room the header really takes.
 */
@UtilityClass
public class TableHeaderRules {

    /** A selector which ends at a header cell or a table head, as {@code th}, {@code table th} or {@code thead tr}. */
    private static final Pattern HEADER_SELECTOR = Pattern.compile("(^|[\\s>+~(])(th|thead)(?![\\w-])", Pattern.CASE_INSENSITIVE);

    /**
     * The properties which make a header taller. Colors and images do not, and white-space only keeps a header to fewer
     * lines, which the measure errs on the safe side without.
     */
    private static final Set<String> HEIGHT_PROPERTIES = Set.of(
            "font", "font-size", "font-weight", "line-height",
            "padding", "padding-top", "padding-bottom",
            "border", "border-top", "border-bottom", "border-width", "border-top-width", "border-bottom-width");

    /** The rules of the CSS which make a table header taller or lower, as CSS the measure reads, or nothing. */
    public @NotNull String measuredBy(@NotNull String css) {
        if (!HEADER_SELECTOR.matcher(css).find()) {
            return "";
        }
        CascadingStyleSheet stylesheet = CSSReader.readFromStringReader(css, new CSSReaderSettings()
                .setBrowserCompliantMode(true)
                .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
                .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback()));
        if (stylesheet == null) {
            return "";
        }
        CSSWriterSettings writerSettings = new CSSWriterSettings();
        StringBuilder rules = new StringBuilder();
        CSSVisitor.visitCSS(stylesheet, new DefaultCSSVisitor() {
            @Override
            public void onBeginStyleRule(@NotNull CSSStyleRule styleRule) {
                List<String> selectors = new ArrayList<>();
                for (CSSSelector selector : styleRule.getAllSelectors()) {
                    String text = selector.getAsCSSString(writerSettings, 0);
                    if (HEADER_SELECTOR.matcher(text).find()) {
                        selectors.add(text);
                    }
                }
                List<String> declarations = new ArrayList<>();
                for (CSSDeclaration declaration : styleRule.getAllDeclarations()) {
                    if (makesTheHeaderTaller(declaration)) {
                        declarations.add(declaration.getAsCSSString(writerSettings, 0));
                    }
                }
                if (!selectors.isEmpty() && !declarations.isEmpty()) {
                    rules.append(String.join(", ", selectors)).append(" { ").append(String.join("; ", declarations)).append("; }\n");
                }
            }
        });
        return rules.toString();
    }

    private static boolean makesTheHeaderTaller(@NotNull CSSDeclaration declaration) {
        // The measure loads nothing: a value which names a resource is left to the export
        return HEIGHT_PROPERTIES.contains(declaration.getProperty().toLowerCase(Locale.ROOT))
                && !declaration.getExpressionAsCSSString().toLowerCase(Locale.ROOT).contains("url(");
    }
}
