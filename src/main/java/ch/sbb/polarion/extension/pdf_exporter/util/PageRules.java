package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.Measure;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSPageRule;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.ICSSPageRuleMember;
import com.helger.css.decl.ICSSTopLevelRule;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Reads from the {@code @page} rules of the CSS of an export how high and how wide a page leaves its content: the height
 * of the paper less the top and the bottom margins, and its width less the left and the right margins, as the page of
 * each paper size and orientation states them.
 */
@UtilityClass
public class PageRules {

    private static final float PX_PER_MM = 96F / 25.4F;

    /** The margin WeasyPrint gives each side of a page where the CSS states none. */
    private static final float DEFAULT_PAGE_MARGIN_PX = 75;

    /** A margin the CSS states in a unit the page does not lay out, as a percentage, which says nothing of the content. */
    private static final float UNREADABLE = Float.NaN;

    /** The height of each paper size, in pixels, upright. */
    private static final Map<PaperSize, Float> PAPER_HEIGHTS = new EnumMap<>(Map.of(
            PaperSize.A5, 210 * PX_PER_MM,
            PaperSize.A4, 297 * PX_PER_MM,
            PaperSize.A3, 420 * PX_PER_MM,
            PaperSize.B5, 250 * PX_PER_MM,
            PaperSize.B4, 353 * PX_PER_MM,
            PaperSize.JIS_B5, 257 * PX_PER_MM,
            PaperSize.JIS_B4, 364 * PX_PER_MM,
            PaperSize.LETTER, 11 * 96F,
            PaperSize.LEGAL, 14 * 96F,
            PaperSize.LEDGER, 17 * 96F));

    /** The width of each paper size, in pixels, upright, which is the height of its landscape page. */
    private static final Map<PaperSize, Float> PAPER_WIDTHS = new EnumMap<>(Map.of(
            PaperSize.A5, 148 * PX_PER_MM,
            PaperSize.A4, 210 * PX_PER_MM,
            PaperSize.A3, 297 * PX_PER_MM,
            PaperSize.B5, 176 * PX_PER_MM,
            PaperSize.B4, 250 * PX_PER_MM,
            PaperSize.JIS_B5, 182 * PX_PER_MM,
            PaperSize.JIS_B4, 257 * PX_PER_MM,
            PaperSize.LETTER, 8.5F * 96F,
            PaperSize.LEGAL, 8.5F * 96F,
            PaperSize.LEDGER, 11 * 96F));

    /** The margins a page states, the ones it leaves unstated taken from the page every page is. */
    private record Margins(@Nullable Float top, @Nullable Float right, @Nullable Float bottom, @Nullable Float left) {
        private static final Margins NONE = new Margins(null, null, null, null);

        Margins over(@NotNull Margins base) {
            return new Margins(top != null ? top : base.top, right != null ? right : base.right,
                    bottom != null ? bottom : base.bottom, left != null ? left : base.left);
        }

        /** These margins with the ones the declaration states. */
        Margins with(@NotNull CSSDeclaration declaration) {
            String[] values = declaration.getExpressionAsCSSString().trim().split("\\s+");
            return switch (declaration.getProperty().toLowerCase(Locale.ROOT)) {
                case "margin" -> ofShorthand(values);
                case "margin-top" -> new Margins(stated(values[0]), right, bottom, left);
                case "margin-right" -> new Margins(top, stated(values[0]), bottom, left);
                case "margin-bottom" -> new Margins(top, right, stated(values[0]), left);
                case "margin-left" -> new Margins(top, right, bottom, stated(values[0]));
                // Other declarations of a page change no length of its content
                default -> this;
            };
        }

        /** One value for all sides, two for top and bottom then the sides, three or four clockwise from the top. */
        private static Margins ofShorthand(@NotNull String[] values) {
            float right = stated(values[values.length > 1 ? 1 : 0]);
            float left = values.length > 3 ? stated(values[3]) : right;
            return new Margins(stated(values[0]), right, stated(values[values.length > 2 ? 2 : 0]), left);
        }

        /** The length in pixels of a margin the CSS states, or {@link #UNREADABLE} where it is not in an absolute unit. */
        private static float stated(@NotNull String value) {
            Float length = pixels(value);
            return length != null ? length : UNREADABLE;
        }

        /** The length in pixels where it is stated in an absolute unit, or nothing. */
        private static @Nullable Float pixels(@NotNull String value) {
            String length = value.toLowerCase(Locale.ROOT);
            if ("0".equals(length)) {
                return 0F;
            }
            for (Map.Entry<String, Float> unit : Measure.ABSOLUTE_UNITS_IN_PX.entrySet()) {
                if (length.endsWith(unit.getKey())) {
                    try {
                        return Float.parseFloat(length.substring(0, length.length() - unit.getKey().length())) * unit.getValue();
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /**
     * The heights each paper size and orientation leaves the content, by the name the export gives that page, as
     * {@code portA4}. A page whose margins the CSS does not state is not in them.
     *
     * @param ofEveryPage of the page every page is, which a document without page breaks is printed on
     * @param ofNamedPage of the page of its own name, which the export gives each area between page breaks
     */
    public record Heights(@NotNull Map<String, Integer> ofEveryPage, @NotNull Map<String, Integer> ofNamedPage) {
        public static final Heights NONE = new Heights(Map.of(), Map.of());
    }

    /**
     * The widths each paper size and orientation leaves the content, by the name the export gives that page, as
     * {@code portA4}. A page whose side margins the CSS does not state is not in them.
     *
     * @param ofEveryPage of the page every page is, which a document without page breaks is printed on
     * @param ofNamedPage of the page of its own name, which the export gives each area between page breaks
     */
    public record Widths(@NotNull Map<String, Integer> ofEveryPage, @NotNull Map<String, Integer> ofNamedPage) {
        public static final Widths NONE = new Widths(Map.of(), Map.of());
    }

    /** The length of the content a page gives: its height, between the top and the bottom margins, or its width. */
    private enum Direction { DOWN, ACROSS }

    /** The height each page leaves its content, as the {@code @page} rules of the CSS state it. */
    public @NotNull Heights contentHeights(@NotNull String css) {
        return css.contains("@page") ? contentHeights(ExportStylesheet.read(css)) : Heights.NONE;
    }

    /** The height each page leaves its content, as the {@code @page} rules of the stylesheet state it. */
    public @NotNull Heights contentHeights(@Nullable CascadingStyleSheet stylesheet) {
        if (stylesheet == null) {
            return Heights.NONE;
        }
        return contentLengths(stylesheet, Direction.DOWN, Heights::new);
    }

    /** The width each page leaves its content, as the {@code @page} rules of the CSS state it. */
    public @NotNull Widths contentWidths(@NotNull String css) {
        return contentWidths(ExportStylesheet.read(css));
    }

    /** The width each page leaves its content, as the {@code @page} rules of the stylesheet state it. */
    public @NotNull Widths contentWidths(@Nullable CascadingStyleSheet stylesheet) {
        if (stylesheet == null) {
            return Widths.NONE;
        }
        return contentLengths(stylesheet, Direction.ACROSS, Widths::new);
    }

    /** One length of the content of each page, of the page every page is and of each named page. */
    private static <T> @NotNull T contentLengths(@NotNull CascadingStyleSheet stylesheet, @NotNull Direction direction,
                                                 @NotNull BiFunction<Map<String, Integer>, Map<String, Integer>, T> lengths) {
        Map<String, Margins> named = new HashMap<>();
        Margins base = marginsOfThePages(stylesheet, named);
        Map<String, Integer> ofEveryPage = new HashMap<>();
        Map<String, Integer> ofNamedPage = new HashMap<>();
        for (PaperSize size : PaperSize.values()) {
            for (Orientation orientation : Orientation.values()) {
                String name = (orientation == Orientation.LANDSCAPE ? "land" : "port") + size.name();
                float paper = paper(size, orientation, direction);
                putLength(ofEveryPage, name, content(paper, base, direction));
                putLength(ofNamedPage, name, content(paper, named.getOrDefault(name, Margins.NONE).over(base), direction));
            }
        }
        return lengths.apply(ofEveryPage, ofNamedPage);
    }

    /**
     * The margins of the page every page is, which it returns, and those of each page of its own name, which it puts in the
     * given map. A later rule states over an earlier one, as in CSS.
     */
    private static @NotNull Margins marginsOfThePages(@NotNull CascadingStyleSheet stylesheet, @NotNull Map<String, Margins> named) {
        Margins base = Margins.NONE;
        for (ICSSTopLevelRule rule : stylesheet.getAllRules()) {
            if (!(rule instanceof CSSPageRule pageRule)) {
                continue;
            }
            List<String> selectors = pageRule.getAllSelectors();
            if (selectors.isEmpty()) {
                base = marginsOf(pageRule).over(base);
            } else if (selectors.size() == 1 && !selectors.getFirst().startsWith(":")) {
                // A page of its own name, as portA4; :first, :left and :right change no length of the content
                named.merge(selectors.getFirst(), marginsOf(pageRule), (earlier, later) -> later.over(earlier));
            }
        }
        return base;
    }

    /** The length of the paper in the direction: the height of a portrait page is the long side of the paper. */
    private static float paper(@NotNull PaperSize size, @NotNull Orientation orientation, @NotNull Direction direction) {
        boolean longSide = (direction == Direction.DOWN) == (orientation == Orientation.PORTRAIT);
        return longSide ? PAPER_HEIGHTS.get(size) : PAPER_WIDTHS.get(size);
    }

    /**
     * The paper less the two margins across the direction. A height needs both margins stated. A width takes the margin
     * of WeasyPrint for a side the CSS states none of, as the content always has the margins of its page beside it.
     */
    private static @Nullable Integer content(float paper, @NotNull Margins margins, @NotNull Direction direction) {
        Float before = direction == Direction.DOWN ? margins.top() : orDefault(margins.left());
        Float after = direction == Direction.DOWN ? margins.bottom() : orDefault(margins.right());
        return before != null && after != null && !before.isNaN() && !after.isNaN() ? (int) (paper - before - after) : null;
    }

    private static @NotNull Float orDefault(@Nullable Float margin) {
        return margin != null ? margin : DEFAULT_PAGE_MARGIN_PX;
    }

    private static void putLength(@NotNull Map<String, Integer> lengths, @NotNull String name, @Nullable Integer length) {
        if (length != null) {
            lengths.put(name, length);
        }
    }

    private static @NotNull Margins marginsOf(@NotNull CSSPageRule rule) {
        Margins margins = Margins.NONE;
        for (ICSSPageRuleMember member : rule.getAllMembers()) {
            if (member instanceof CSSDeclaration declaration) {
                margins = margins.with(declaration);
            }
        }
        return margins;
    }
}
