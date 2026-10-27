package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class FormulaTextLayerTest {

    private static final String INLINE_FORMULA = "<p>Text <img class=\"polarion-rte-formula\" src=\"data:image/svg+xml;base64,abc\" "
            + "style=\"width:23.648ex; height:6.176ex; vertical-align:-2.505ex;\" data-source=\"x^2 + \\frac{1}{2}\" /> text</p>";

    private static final String BLOCK_FORMULA = "<p><img class=\"polarion-rte-formula\" src=\"data:image/svg+xml;base64,abc\" "
            + "style=\"width:23.648ex; display:block; padding:5px 0px; margin-left:auto; margin-right:auto;\" "
            + "data-source=\"f(x) = \\begin{cases}&lt;br/&gt; &thinsp;x^2, &amp; x \\ge 0&lt;br/&gt;\\end{cases}\" /></p>";

    @Test
    void laysTheSourceOverAFormulaInALine() {
        Document document = JSoupUtils.parseHtml(INLINE_FORMULA);

        FormulaTextLayer.addTo(document, null);

        Element formula = document.selectFirst("img.polarion-rte-formula");
        assertThat(formula).isNotNull();
        Element box = formula.parent();
        assertThat(box).isNotNull();
        assertThat(box.tagName()).isEqualTo("span");
        // The box takes the place of the formula in its line, the formula fills the box
        assertThat(box.attr("style")).contains("position: relative;", "display: inline-block;", "vertical-align: -2.505ex;");
        assertThat(formula.attr("style")).contains("display:block").doesNotContain("vertical-align");

        Element layer = box.selectFirst("span." + FormulaTextLayer.LAYER_CLASS);
        assertThat(layer).isNotNull();
        assertThat(layer.text()).isEqualTo("x^2 + \\frac{1}{2}");
        assertThat(layer.attr("style")).contains("position: absolute;", "color: transparent;", "overflow: hidden;");
        assertThat(document.selectFirst("p").text()).startsWith("Text").endsWith("text");
    }

    @Test
    void laysTheSourceOverAFormulaOfItsOwnLine() {
        Document document = JSoupUtils.parseHtml(BLOCK_FORMULA);

        FormulaTextLayer.addTo(document, PdfVariant.PDF_A_2B);

        Element formula = document.selectFirst("img.polarion-rte-formula");
        assertThat(formula).isNotNull();
        Element box = formula.parent();
        assertThat(box).isNotNull();
        // The box is centered as the formula was, which keeps its own style
        assertThat(box.attr("style")).contains("display: table;", "margin-left: auto;", "margin-right: auto;");
        assertThat(formula.attr("style")).contains("display:block").contains("margin-left:auto");
        assertThat(box.selectFirst("span." + FormulaTextLayer.LAYER_CLASS).text()).isEqualTo("f(x) = \\begin{cases} x^2, & x \\ge 0 \\end{cases}");
    }

    @ParameterizedTest
    @EnumSource(value = PdfVariant.class, names = {"PDF_A_1A", "PDF_A_1B"})
    void leavesTheFormulasOfPdfA1AsTheyAre(PdfVariant variant) {
        Document document = JSoupUtils.parseHtml(INLINE_FORMULA + BLOCK_FORMULA);
        String before = document.body().html();

        FormulaTextLayer.addTo(document, variant);

        assertThat(document.body().html()).isEqualTo(before);
    }

    @Test
    void leavesAFormulaWithoutSourceAsItIs() {
        Document document = JSoupUtils.parseHtml("<p><img class=\"polarion-rte-formula\" src=\"a.svg\" data-source=\" &lt;br/&gt; \" />"
                + "<img class=\"polarion-rte-formula\" src=\"b.svg\" /><img src=\"c.svg\" data-source=\"x\" /></p>");
        String before = document.body().html();

        FormulaTextLayer.addTo(document, null);

        assertThat(document.body().html()).isEqualTo(before);
    }

    @Test
    void writesTheSourceInOneLine() {
        assertThat(FormulaTextLayer.sourceOf("a<br/>b<BR>c <br /> d\u2009\u2009e\n\tf ")).isEqualTo("a b c d e f");
    }
}
