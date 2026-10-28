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

    /** The side margins and paddings of the body, as the last rule for print which states each states it. */
    private static final class Declarations extends DefaultCSSVisitor {
        private static final int MARGIN_LEFT = 0;
        private static final int MARGIN_RIGHT = 1;
        private static final int PADDING_LEFT = 2;
        private static final int PADDING_RIGHT = 3;

        /** A selector of the body itself, alone or after its ancestors, as {@code html > body}. */
        private static final Pattern BODY = Pattern.compile("(^|.*[\\s>+~])body");

        private final CSSWriterSettings writerSettings = new CSSWriterSettings();
        /** Whether each media rule the visit is in applies to print. */
        private final Deque<Boolean> media = new ArrayDeque<>();
        private final float[] lengths = {DEFAULT_MARGIN_PX, DEFAULT_MARGIN_PX, 0, 0};
        /** Whether the length of each side was stated {@code !important}, which a later declaration does not override. */
        private final boolean[] important = new boolean[lengths.length];

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
                if (BODY.matcher(selector.getAsCSSString(writerSettings, 0).trim().toLowerCase(Locale.ROOT)).matches()) {
                    styleRule.getAllDeclarations().forEach(this::read);
                }
            }
        }

        private static boolean appliesToPrint(@NotNull CSSMediaQuery query) {
            String medium = query.getMedium();
            String name = medium != null ? medium.toLowerCase(Locale.ROOT) : "all";
            return query.isNot() != ("all".equals(name) || "print".equals(name));
        }

        private int sum() {
            return Math.round(lengths[MARGIN_LEFT] + lengths[MARGIN_RIGHT] + lengths[PADDING_LEFT] + lengths[PADDING_RIGHT]);
        }

        private void read(@NotNull CSSDeclaration declaration) {
            String property = declaration.getProperty().toLowerCase(Locale.ROOT);
            String[] values = declaration.getExpressionAsCSSString().trim().toLowerCase(Locale.ROOT).split("\\s+");
            boolean isImportant = declaration.isImportant();
            switch (property) {
                case "margin" -> {
                    set(MARGIN_RIGHT, right(values), isImportant);
                    set(MARGIN_LEFT, left(values), isImportant);
                }
                case "margin-left" -> set(MARGIN_LEFT, values[0], isImportant);
                case "margin-right" -> set(MARGIN_RIGHT, values[0], isImportant);
                case "padding" -> {
                    set(PADDING_RIGHT, right(values), isImportant);
                    set(PADDING_LEFT, left(values), isImportant);
                }
                case "padding-left" -> set(PADDING_LEFT, values[0], isImportant);
                case "padding-right" -> set(PADDING_RIGHT, values[0], isImportant);
                default -> {
                    // Other declarations of the body take no width beside its content
                }
            }
        }

        /**
         * States the length of a side, unless an earlier declaration stated it {@code !important} and this one does not. A
         * length in a unit the page does not lay out, as a percentage, leaves the one before it.
         */
        private void set(int side, @NotNull String value, boolean isImportant) {
            Float length = pixels(value);
            if (length == null || important[side] && !isImportant) {
                return;
            }
            lengths[side] = length;
            important[side] = isImportant;
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
