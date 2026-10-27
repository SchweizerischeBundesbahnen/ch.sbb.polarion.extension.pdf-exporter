package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A reader selects and finds a formula by its LaTeX source.
 * <p>
 * Polarion draws a formula as an SVG of MathJax, whose glyphs are paths. Its source lies over it as transparent text,
 * within the box of the formula, and the text of the page around it stays where it was.
 * </p>
 */
class FormulaTextTest extends BasePdfConverterTest {

    /** A formula as MathJax draws it: paths, no text. */
    private static final String FORMULA = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString("""
            <svg xmlns="http://www.w3.org/2000/svg" width="20ex" height="3ex" viewBox="0 0 2000 300"><path d="M0 0H2000V300H0Z" fill="none" stroke="black" stroke-width="20"/></svg>"""
            .getBytes(StandardCharsets.UTF_8));

    private static final String SOURCE = "\\frac{1}{2\\pi i} \\int_{-\\infty}^{\\infty} x^2 dx";

    private static final String CONTENT = """
            <div class="polarion-dle-workitem-basic-0">Before <img class="polarion-rte-formula" src="%1$s" style="width:20ex; height:3ex; vertical-align:-1ex;" data-source="%2$s"/> after</div>
            <p><img class="polarion-rte-formula" src="%1$s" style="width:20ex; height:3ex; display:block; padding:5px 0px; margin-left:auto; margin-right:auto;" data-source="%2$s&lt;br/&gt;"/></p>
            <p>Below</p>""";

    /** How far a letter of the source may stand out of the box of its formula, in points. */
    private static final float TOLERANCE_PT = 1f;

    @Test
    void laysTheSourceOfEachFormulaOverIt() {
        byte[] pdf = export(null);

        List<DrawnImages.Box> formulas = DrawnImages.boxesIn(pdf);
        List<List<TextPosition>> sources = linesWith(pdf, "\\frac");
        assertThat(formulas).hasSize(2);
        assertThat(sources).hasSize(2);
        for (int index = 0; index < formulas.size(); index++) {
            DrawnImages.Box formula = formulas.get(index);
            TextPosition first = sources.get(index).get(0);
            assertThat(first.getXDirAdj()).as("The source of formula %d starts in its box", index).isBetween(formula.left() - TOLERANCE_PT, formula.left() + formula.width());
            assertThat(first.getYDirAdj()).as("The source of formula %d stands in its box", index).isBetween(formula.top() - TOLERANCE_PT, formula.top() + formula.height() + TOLERANCE_PT);
        }
        assertThat(textOf(pdf)).contains(SOURCE, "Before", "after", "Below");
    }

    @Test
    void leavesTheFormulasOfPdfA1WithoutText() {
        byte[] pdf = export(PdfVariant.PDF_A_1B);

        assertThat(textOf(pdf)).doesNotContain("\\frac").contains("Before", "after", "Below");
    }

    private byte @NotNull [] export(@Nullable PdfVariant variant) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .pdfVariant(variant)
                .build();
        return exportLiveDoc("Formulas", CONTENT.formatted(FORMULA, SOURCE), params);
    }

    @SneakyThrows
    private static @NotNull String textOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /** The letters of each line of text which holds the needle, from the needle on. */
    @SneakyThrows
    private static @NotNull List<List<TextPosition>> linesWith(byte @NotNull [] pdf, @NotNull String needle) {
        List<List<TextPosition>> lines = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    int start = text.indexOf(needle);
                    if (start >= 0) {
                        lines.add(positions.subList(start, positions.size()));
                    }
                }
            }.getText(document);
        }
        return lines;
    }
}
