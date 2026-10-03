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

/**
 * Reads from the {@code @page} rules of the CSS of an export how high a page leaves its content: the height of the paper
 * less the top and the bottom margins, as the page of each paper size and orientation states them.
 */
@UtilityClass
public class PageRules {

    private static final float PX_PER_MM = 96F / 25.4F;

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
    private record Margins(@Nullable Float top, @Nullable Float bottom) {
        private static final Margins NONE = new Margins(null, null);

        Margins over(@NotNull Margins base) {
            return new Margins(top != null ? top : base.top, bottom != null ? bottom : base.bottom);
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

    /** The height each page leaves its content, as the {@code @page} rules of the CSS state it. */
    public @NotNull Heights contentHeights(@NotNull String css) {
        return css.contains("@page") ? contentHeights(ExportStylesheet.read(css)) : Heights.NONE;
    }

    /** The height each page leaves its content, as the {@code @page} rules of the stylesheet state it. */
    public @NotNull Heights contentHeights(@Nullable CascadingStyleSheet stylesheet) {
        if (stylesheet == null) {
            return Heights.NONE;
        }
        Map<String, Margins> named = new HashMap<>();
        Margins base = marginsOfThePages(stylesheet, named);
        Map<String, Integer> ofEveryPage = new HashMap<>();
        Map<String, Integer> ofNamedPage = new HashMap<>();
        for (PaperSize size : PaperSize.values()) {
            for (Orientation orientation : Orientation.values()) {
                String name = (orientation == Orientation.LANDSCAPE ? "land" : "port") + size.name();
                float paper = orientation == Orientation.LANDSCAPE ? PAPER_WIDTHS.get(size) : PAPER_HEIGHTS.get(size);
                putHeight(ofEveryPage, name, paper, base);
                putHeight(ofNamedPage, name, paper, named.getOrDefault(name, Margins.NONE).over(base));
            }
        }
        return new Heights(ofEveryPage, ofNamedPage);
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
                // A page of its own name, as portA4; :first, :left and :right change no height of the content
                named.merge(selectors.getFirst(), marginsOf(pageRule), (earlier, later) -> later.over(earlier));
            }
        }
        return base;
    }

    private static void putHeight(@NotNull Map<String, Integer> heights, @NotNull String name, float paper, @NotNull Margins margins) {
        if (margins.top() != null && margins.bottom() != null) {
            heights.put(name, (int) (paper - margins.top() - margins.bottom()));
        }
    }

    private static @NotNull Margins marginsOf(@NotNull CSSPageRule rule) {
        Float top = null;
        Float bottom = null;
        for (ICSSPageRuleMember member : rule.getAllMembers()) {
            if (member instanceof CSSDeclaration declaration) {
                String property = declaration.getProperty().toLowerCase(Locale.ROOT);
                String[] values = declaration.getExpressionAsCSSString().trim().split("\\s+");
                switch (property) {
                    case "margin" -> {
                        top = pixels(values[0]);
                        bottom = pixels(values.length > 2 ? values[2] : values[0]);
                    }
                    case "margin-top" -> top = pixels(values[0]);
                    case "margin-bottom" -> bottom = pixels(values[0]);
                    default -> {
                        // Other declarations of a page change no height of its content
                    }
                }
            }
        }
        return new Margins(top, bottom);
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
