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
 * Finds where the CSS of an export turns automatic hyphenation off, so that the words of a table there keep their
 * break points.
 */
@UtilityClass
public class HyphenationRules {

    /** Every element: a stylesheet which cannot be read may turn hyphenation off anywhere. */
    public static final String EVERYWHERE = "*";

    private static final Set<String> HYPHENS = Set.of("hyphens", "-webkit-hyphens");

    private static final Set<String> OFF = Set.of("none", "manual");

    /** A cheap look for a declaration of the kind, which spares the parser the stylesheets without one. */
    private static final Pattern MAY_TURN_OFF = Pattern.compile("hyphens\\s*:\\s*(none|manual)", Pattern.CASE_INSENSITIVE);

    /** The selectors of the rules which set {@code hyphens} to {@code none} or {@code manual}, at any depth. */
    public @NotNull List<String> turningHyphenationOff(@NotNull String css) {
        if (!MAY_TURN_OFF.matcher(css).find()) {
            return List.of();
        }
        CascadingStyleSheet stylesheet = CSSReader.readFromStringReader(css, new CSSReaderSettings()
                .setBrowserCompliantMode(true)
                .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
                .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback()));
        if (stylesheet == null) {
            return List.of(EVERYWHERE);
        }
        List<String> selectors = new ArrayList<>();
        CSSWriterSettings writerSettings = new CSSWriterSettings();
        CSSVisitor.visitCSS(stylesheet, new DefaultCSSVisitor() {
            @Override
            public void onBeginStyleRule(@NotNull CSSStyleRule styleRule) {
                if (styleRule.getAllDeclarations().containsAny(HyphenationRules::turnsOff)) {
                    for (CSSSelector selector : styleRule.getAllSelectors()) {
                        selectors.add(selector.getAsCSSString(writerSettings, 0));
                    }
                }
            }
        });
        return selectors;
    }

    private static boolean turnsOff(@NotNull CSSDeclaration declaration) {
        return HYPHENS.contains(declaration.getProperty().toLowerCase(Locale.ROOT))
                && OFF.contains(declaration.getExpressionAsCSSString().trim().toLowerCase(Locale.ROOT));
    }
}
