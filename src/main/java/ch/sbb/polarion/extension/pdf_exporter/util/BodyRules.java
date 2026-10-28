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
import java.util.regex.Pattern;

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
        return declarations.sum();
    }

    /**
     * The side margins and paddings of the body, as the cascade of the rules for print gives each: an {@code !important}
     * declaration over one which is not, then the one of the more specific selector, then the later one.
     */
    private static final class Declarations extends DefaultCSSVisitor {
        private static final int MARGIN_LEFT = 0;
        private static final int MARGIN_RIGHT = 1;
        private static final int PADDING_LEFT = 2;
        private static final int PADDING_RIGHT = 3;

        /**
         * A selector which matches the body of an export whatever the document: the body, alone or after the root element,
         * as {@code html > body} or {@code :root body}. The body of an export has no other ancestor.
         */
        private static final Pattern BODY = Pattern.compile("((html|:root)(\\s*>\\s*|\\s+))*body");

        /** The weight of {@code !important} over any specificity. */
        private static final int IMPORTANT = 1_000_000;

        /** The weight of a pseudo-class, as {@code :root}, over a type selector. */
        private static final int PSEUDO_CLASS = 1_000;

        /** The weight of what WeasyPrint gives the body, below any rule of the CSS. */
        private static final int USER_AGENT = -1;

        private final CSSWriterSettings writerSettings = new CSSWriterSettings();
        /** Whether each media rule the visit is in applies to print. */
        private final Deque<Boolean> media = new ArrayDeque<>();
        private final float[] lengths = {DEFAULT_MARGIN_PX, DEFAULT_MARGIN_PX, 0, 0};
        /** The weight of the declaration which stated the length of each side, which a lighter one does not override. */
        private final int[] weights = {USER_AGENT, USER_AGENT, USER_AGENT, USER_AGENT};

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
            // Of the selectors of the rule which match the body, the most specific one counts
            int specificity = USER_AGENT;
            for (CSSSelector selector : styleRule.getAllSelectors()) {
                String text = selector.getAsCSSString(writerSettings, 0).trim().toLowerCase(Locale.ROOT);
                if (BODY.matcher(text).matches()) {
                    specificity = Math.max(specificity, specificity(text));
                }
            }
            if (specificity != USER_AGENT) {
                int weight = specificity;
                styleRule.getAllDeclarations().forEach(declaration -> read(declaration, weight));
            }
        }

        /** The specificity of a selector of the body: its pseudo-classes, then its type selectors. */
        private static int specificity(@NotNull String selector) {
            int pseudoClasses = selector.split(":root", -1).length - 1;
            int types = selector.replace(":root", "").split("html|body", -1).length - 1;
            return pseudoClasses * PSEUDO_CLASS + types;
        }

        private static boolean appliesToPrint(@NotNull CSSMediaQuery query) {
            String medium = query.getMedium();
            String name = medium != null ? medium.toLowerCase(Locale.ROOT) : "all";
            return query.isNot() != ("all".equals(name) || "print".equals(name));
        }

        private int sum() {
            return Math.round(lengths[MARGIN_LEFT] + lengths[MARGIN_RIGHT] + lengths[PADDING_LEFT] + lengths[PADDING_RIGHT]);
        }

        private void read(@NotNull CSSDeclaration declaration, int specificity) {
            String property = declaration.getProperty().toLowerCase(Locale.ROOT);
            String[] values = declaration.getExpressionAsCSSString().trim().toLowerCase(Locale.ROOT).split("\\s+");
            int weight = declaration.isImportant() ? IMPORTANT + specificity : specificity;
            switch (property) {
                case "margin" -> {
                    set(MARGIN_RIGHT, right(values), weight);
                    set(MARGIN_LEFT, left(values), weight);
                }
                case "margin-left" -> set(MARGIN_LEFT, values[0], weight);
                case "margin-right" -> set(MARGIN_RIGHT, values[0], weight);
                case "padding" -> {
                    set(PADDING_RIGHT, right(values), weight);
                    set(PADDING_LEFT, left(values), weight);
                }
                case "padding-left" -> set(PADDING_LEFT, values[0], weight);
                case "padding-right" -> set(PADDING_RIGHT, values[0], weight);
                default -> {
                    // Other declarations of the body take no width beside its content
                }
            }
        }

        /**
         * States the length of a side, unless an earlier declaration of more weight stated it. A length in a unit the page
         * does not lay out, as a percentage, leaves the one before it.
         */
        private void set(int side, @NotNull String value, int weight) {
            Float length = pixels(value);
            if (length == null || weight < weights[side]) {
                return;
            }
            lengths[side] = length;
            weights[side] = weight;
        }

        /** The right side of a shorthand: its only value, or its second one. */
        private static @NotNull String right(@NotNull String[] values) {
            return values[values.length > 1 ? 1 : 0];
        }

        /** The left side of a shorthand: its fourth value, or the one of the right side. */
        private static @NotNull String left(@NotNull String[] values) {
            return values.length > 3 ? values[3] : right(values);
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
}
