package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.property.ECSSProperty;
import com.helger.css.propertyvalue.CCSSValue;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lays the LaTeX source of each formula over it as invisible text, as an OCR layer lies over a scanned page. Polarion draws a
 * formula as an SVG of MathJax, whose glyphs are paths: without the layer a reader can neither select nor find the formula.
 * The text is transparent, so it prints nothing, and it is clipped to the formula, so it takes no room of the page.
 */
@UtilityClass
public class FormulaTextLayer {

    /** The attribute of a formula which holds its LaTeX source. */
    private static final String SOURCE = "data-source";

    /** The class of the images Polarion draws its formulas as. */
    public static final String FORMULA_CLASS = "polarion-rte-formula";

    /** The formulas Polarion draws, with their LaTeX source. */
    private static final String FORMULA = "img." + FORMULA_CLASS + "[" + SOURCE + "]";

    /** The class of the text laid over a formula. */
    public static final String LAYER_CLASS = "pdf-exporter-formula-text";

    /** PDF/A-1 allows no transparency (ISO 19005-1, 6.4), so its formulas stay without the layer. */
    private static final Set<PdfVariant> WITHOUT_TRANSPARENCY = Set.of(PdfVariant.PDF_A_1A, PdfVariant.PDF_A_1B);

    /** Polarion writes the line breaks of a source as {@code <br/>} into the attribute. */
    private static final Pattern LINE_BREAK = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

    /** Any run of white space, the thin spaces Polarion indents a source with included. */
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s\\u2009\\u00A0]+");

    private static final String LAYER_STYLE = "position: absolute; top: 0; left: 0; right: 0; bottom: 0; overflow: hidden; color: transparent; "
            + "font-size: 4pt; line-height: 1; white-space: normal; overflow-wrap: anywhere; hyphens: none;";

    public void addTo(@NotNull Document document, @Nullable PdfVariant pdfVariant) {
        if (pdfVariant != null && WITHOUT_TRANSPARENCY.contains(pdfVariant)) {
            return;
        }
        for (Element formula : document.select(FORMULA)) {
            String source = sourceOf(formula.attr(SOURCE));
            if (!source.isEmpty()) {
                wrap(formula, source);
            }
        }
    }

    /** The source as one line: the line breaks and indents of the editor say nothing to a reader of the PDF. */
    @VisibleForTesting
    @NotNull String sourceOf(@NotNull String attribute) {
        return WHITE_SPACE.matcher(LINE_BREAK.matcher(attribute).replaceAll(" ")).replaceAll(" ").trim();
    }

    /**
     * Wraps the formula into a box of its own size, which the layer fills. A formula of its own line keeps its place by its
     * margins, which the box takes over; one in a line keeps it by its vertical alignment, which the box takes over too, as a
     * box without lines of text stands on its bottom edge, as an image does.
     */
    private void wrap(@NotNull Element formula, @NotNull String source) {
        CSSDeclarationList styles = CssUtils.parseDeclarations(formula.attr(HtmlTagAttr.STYLE));
        StringBuilder box = new StringBuilder(CssUtils.declaration(ECSSProperty.POSITION, CCSSValue.RELATIVE));
        if (CCSSValue.BLOCK.equals(CssUtils.getPropertyValue(styles, ECSSProperty.DISPLAY))) {
            box.append(' ').append(CssUtils.declaration(ECSSProperty.DISPLAY, CCSSValue.TABLE));
            box.append(' ').append(CssUtils.declaration(ECSSProperty.MARGIN_LEFT, valueOr(styles, ECSSProperty.MARGIN_LEFT, "0")));
            box.append(' ').append(CssUtils.declaration(ECSSProperty.MARGIN_RIGHT, valueOr(styles, ECSSProperty.MARGIN_RIGHT, "0")));
        } else {
            box.append(' ').append(CssUtils.declaration(ECSSProperty.DISPLAY, CCSSValue.INLINE_BLOCK));
            box.append(' ').append(CssUtils.declaration(ECSSProperty.VERTICAL_ALIGN, valueOr(styles, ECSSProperty.VERTICAL_ALIGN, CCSSValue.BASELINE)));
            CssUtils.removeProperty(styles, ECSSProperty.VERTICAL_ALIGN);
            CssUtils.setPropertyValue(styles, ECSSProperty.DISPLAY, CCSSValue.BLOCK);
            formula.attr(HtmlTagAttr.STYLE, styles.getAsCSSString());
        }

        Element wrapper = new Element(HtmlTag.SPAN).attr(HtmlTagAttr.STYLE, box.toString());
        formula.before(wrapper);
        wrapper.appendChild(formula);
        wrapper.appendElement(HtmlTag.SPAN).addClass(LAYER_CLASS).attr(HtmlTagAttr.STYLE, LAYER_STYLE).text(source);
    }

    private @NotNull String valueOr(@NotNull CSSDeclarationList styles, @NotNull ECSSProperty property, @NotNull String fallback) {
        String value = CssUtils.getPropertyValue(styles, property);
        return value.isEmpty() ? fallback : value;
    }
}
