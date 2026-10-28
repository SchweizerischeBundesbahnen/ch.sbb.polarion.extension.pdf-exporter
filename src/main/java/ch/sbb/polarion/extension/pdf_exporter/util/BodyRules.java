package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSMediaQuery;
import com.helger.css.decl.CSSMediaRule;
import com.helger.css.decl.CSSSelector;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.visit.DefaultCSSVisitor;
import com.helger.css.writer.CSSWriterSettings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;

/**
 * Reads from the CSS of an export how much of the width of a page the body takes beside its content: its left and right
 * margins and paddings. A table of the content has the page less that.
 */
@UtilityClass
public class BodyRules {

    /** The margin WeasyPrint gives the body on each side where the CSS states none, as a browser does. */
    private static final float DEFAULT_MARGIN_PX = 8;

    /** The width the body takes beside its content where nothing of the CSS is known: the default margins. */
    public static final int DEFAULT_SIDES_PX = (int) (2 * DEFAULT_MARGIN_PX);

    /** The width the body takes beside its content, in pixels, as the CSS states its side margins and paddings. */
    public int sides(@NotNull String css) {
        return sides(ExportStylesheet.read(css));
    }

    /** The width the body takes beside its content, in pixels, as the stylesheet states its side margins and paddings. */
    public int sides(@Nullable CascadingStyleSheet stylesheet) {
        if (stylesheet == null) {
            return DEFAULT_SIDES_PX;
        }
        Declarations declarations = new Declarations();
        CSSVisitor.visitCSS(stylesheet, declarations);
        return Math.round(declarations.marginLeft + declarations.marginRight + declarations.paddingLeft + declarations.paddingRight);
    }

    /** The side margins and paddings of the body, as the last rule for print which states each states it. */
    private static final class Declarations extends DefaultCSSVisitor {
        private final CSSWriterSettings writerSettings = new CSSWriterSettings();
        /** Whether each media rule the visit is in applies to print. */
        private final Deque<Boolean> media = new ArrayDeque<>();
        private float marginLeft = DEFAULT_MARGIN_PX;
        private float marginRight = DEFAULT_MARGIN_PX;
        private float paddingLeft;
        private float paddingRight;

        @Override
        public void onBeginMediaRule(@NotNull CSSMediaRule mediaRule) {
            media.push(mediaRule.getAllMediaQueries().stream().anyMatch(Declarations::appliesToPrint));
        }

        @Override
        public void onEndMediaRule(@NotNull CSSMediaRule mediaRule) {
            media.pop();
        }

        @Override
        public void onBeginStyleRule(@NotNull CSSStyleRule styleRule) {
            if (media.contains(Boolean.FALSE)) {
                return;
            }
            for (CSSSelector selector : styleRule.getAllSelectors()) {
                if ("body".equals(selector.getAsCSSString(writerSettings, 0).trim().toLowerCase(Locale.ROOT))) {
                    styleRule.getAllDeclarations().forEach(this::read);
                }
            }
        }

        private static boolean appliesToPrint(@NotNull CSSMediaQuery query) {
            String medium = query.getMedium() != null ? query.getMedium().toLowerCase(Locale.ROOT) : "all";
            return !query.isNot() && ("all".equals(medium) || "print".equals(medium));
        }

        private void read(@NotNull CSSDeclaration declaration) {
            String property = declaration.getProperty().toLowerCase(Locale.ROOT);
            String[] values = declaration.getExpressionAsCSSString().trim().toLowerCase(Locale.ROOT).split("\\s+");
            switch (property) {
                case "margin" -> {
                    marginRight = orKept(pixels(right(values)), marginRight);
                    marginLeft = orKept(pixels(left(values)), marginLeft);
                }
                case "margin-left" -> marginLeft = orKept(pixels(values[0]), marginLeft);
                case "margin-right" -> marginRight = orKept(pixels(values[0]), marginRight);
                case "padding" -> {
                    paddingRight = orKept(pixels(right(values)), paddingRight);
                    paddingLeft = orKept(pixels(left(values)), paddingLeft);
                }
                case "padding-left" -> paddingLeft = orKept(pixels(values[0]), paddingLeft);
                case "padding-right" -> paddingRight = orKept(pixels(values[0]), paddingRight);
                default -> {
                    // Other declarations of the body take no width beside its content
                }
            }
        }

        /** The right side of a shorthand: its only value, or its second one. */
        private static @NotNull String right(@NotNull String[] values) {
            return values[values.length > 1 ? 1 : 0];
        }

        /** The left side of a shorthand: its fourth value, or the one of the right side. */
        private static @NotNull String left(@NotNull String[] values) {
            return values.length > 3 ? values[3] : right(values);
        }

        /** A length the CSS states in a unit the page does not lay out, as a percentage, leaves the one before it. */
        private static float orKept(@Nullable Float length, float kept) {
            return length != null ? length : kept;
        }
    }

    /** The length in pixels where it is stated in an absolute unit, or nothing. */
    private static @Nullable Float pixels(@NotNull String value) {
        if ("0".equals(value)) {
            return 0F;
        }
        for (Map.Entry<String, Float> unit : Measure.ABSOLUTE_UNITS_IN_PX.entrySet()) {
            if (value.endsWith(unit.getKey()) && !Measure.EX.equals(unit.getKey())) {
                try {
                    return Float.parseFloat(value.substring(0, value.length() - unit.getKey().length())) * unit.getValue();
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
