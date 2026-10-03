package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
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
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Reads from the CSS of an export how much room the paragraph an image stands in takes under it: its bottom margin and the
 * descent of its line, below the baseline the image stands on. Its top margin a page drops where the paragraph starts it.
 */
@UtilityClass
public class ParagraphRules {

    /** The font size of the body where the CSS states none, as a browser gives it. */
    private static final float DEFAULT_FONT_SIZE_PX = 16;

    /** How far a line of text reaches below its baseline, its descent and half its leading, in em. */
    private static final float DESCENT_EM = 0.3f;

    /** The room the default CSS leaves: a margin of .8em and the descent of a line of 10pt. */
    public static final int DEFAULT_ROOM_PX = 15;

    /** The room under an image the paragraph it stands in takes, in pixels, as the CSS states its font and its margins. */
    public int roomUnderAnImage(@NotNull String css) {
        CascadingStyleSheet stylesheet = CSSReader.readFromStringReader(css, new CSSReaderSettings()
                .setBrowserCompliantMode(true)
                .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
                .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback()));
        if (stylesheet == null) {
            return DEFAULT_ROOM_PX;
        }
        Declarations declarations = new Declarations();
        CSSVisitor.visitCSS(stylesheet, declarations);
        if (declarations.marginBottom == null) {
            return DEFAULT_ROOM_PX;
        }
        Float font = declarations.fontSize != null ? pixels(declarations.fontSize, DEFAULT_FONT_SIZE_PX) : null;
        float em = font != null ? font : DEFAULT_FONT_SIZE_PX;
        Float margin = pixels(declarations.marginBottom, em);
        return margin != null ? Math.round(margin + DESCENT_EM * em) : DEFAULT_ROOM_PX;
    }

    /** The font size of the body and the bottom margin of a paragraph, as the last rule which states each states it. */
    private static final class Declarations extends DefaultCSSVisitor {
        private final CSSWriterSettings writerSettings = new CSSWriterSettings();
        private String fontSize;
        private String marginBottom;

        @Override
        public void onBeginStyleRule(@NotNull CSSStyleRule styleRule) {
            for (CSSSelector selector : styleRule.getAllSelectors()) {
                String text = selector.getAsCSSString(writerSettings, 0).trim().toLowerCase(Locale.ROOT);
                if ("body".equals(text) || "p".equals(text)) {
                    styleRule.getAllDeclarations().forEach(declaration -> read(text, declaration));
                }
            }
        }

        private void read(@NotNull String selector, @NotNull CSSDeclaration declaration) {
            String property = declaration.getProperty().toLowerCase(Locale.ROOT);
            String value = declaration.getExpressionAsCSSString().trim().toLowerCase(Locale.ROOT);
            if ("body".equals(selector)) {
                if ("font-size".equals(property)) {
                    fontSize = value;
                }
            } else if ("margin-bottom".equals(property)) {
                marginBottom = value;
            } else if ("margin".equals(property)) {
                String[] sides = value.split("\\s+");
                marginBottom = sides.length > 2 ? sides[2] : sides[0];
            }
        }
    }

    /** The length in pixels, where it is stated in an absolute unit or in em of the given size, or nothing. */
    private static @Nullable Float pixels(@NotNull String value, float em) {
        try {
            if ("0".equals(value)) {
                return 0F;
            }
            if (value.endsWith("em") && !value.endsWith("rem")) {
                return Float.parseFloat(value.substring(0, value.length() - 2)) * em;
            }
            for (Map.Entry<String, Float> unit : Measure.ABSOLUTE_UNITS_IN_PX.entrySet()) {
                if (value.endsWith(unit.getKey()) && !Measure.EX.equals(unit.getKey())) {
                    return Float.parseFloat(value.substring(0, value.length() - unit.getKey().length())) * unit.getValue();
                }
            }
        } catch (NumberFormatException e) {
            // A length which is no number says nothing of the room
        }
        return null;
    }
}
