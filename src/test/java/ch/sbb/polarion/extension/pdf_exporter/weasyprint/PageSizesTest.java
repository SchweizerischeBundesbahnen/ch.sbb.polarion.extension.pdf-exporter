package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Fit to page gives a diagram taller than a page the height its page leaves it, on every paper size and orientation the
 * export offers: the margins of the default CSS, not a height of the paper size alone. The diagram fills its page and
 * stays above the line of the footer, alone and in a table row under its header.
 */
class PageSizesTest extends BasePdfConverterTest {

    /** A diagram of the system test's own document, 81x1521 px, taller than any page. */
    private static final String DIAGRAM = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(readImageResource("diagram_20251002-1153.37186.mxg.svg"));

    private static final String CELL = "text-align: left;vertical-align: top;border: 1px solid #CCCCCC;padding: 5px;";

    /** A line of text, the diagram under it, a table whose one row holds the diagram again, and the empty paragraph Polarion ends a document with. */
    private static final String CONTENT = """
            <p>A diagram taller than the page, fitted to it.</p>
            <p><img src="%1$s" style="max-width: 650px;"/></p>
            <table class="polarion-Document-table" style="width: 100%%;border: 1px solid #CCCCCC;border-collapse: collapse;"><tbody>
            <tr><th style="font-weight: bold;background-color: #F0F0F0;%2$s">Diagram 1</th><th style="font-weight: bold;background-color: #F0F0F0;%2$s">Note 1</th></tr>
            <tr><td style="%2$s"><img src="%1$s" style="max-width: 650px;"/></td><td style="%2$s">Taller than a page</td></tr>
            </tbody></table>
            <p id="polarion_14">
              <br />
            </p>""".formatted(DIAGRAM, CELL);

    /** The bottom margin of the page every page is, as the default CSS states it. */
    private static final float BOTTOM_MARGIN_PX = 90;

    /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
    private static final float PT_PER_PX = 0.75f;

    static Stream<Arguments> pages() {
        return Arrays.stream(PaperSize.values()).flatMap(size -> Stream.of(Orientation.PORTRAIT, Orientation.LANDSCAPE).map(orientation -> Arguments.of(size, orientation)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("pages")
    void fitsATallDiagramToThePage(@NotNull PaperSize size, @NotNull Orientation orientation) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(orientation)
                .paperSize(size)
                .fitToPage(true)
                .build();

        byte[] pdf = exportLiveDoc("Page sizes", CONTENT, params);
        boolean differ = compareContentUsingReferenceImages("fitsATallDiagramToThePage_" + size + "_" + orientation, pdf);

        List<DrawnImages.Box> diagrams = DrawnImages.boxesIn(pdf);
        assertThat(diagrams).as("The document draws the diagram twice, whole").hasSize(2);
        float footerLine = footerLineOf(pdf);
        for (DrawnImages.Box diagram : diagrams) {
            assertThat(diagram.top() + diagram.height()).as("The diagram on page %d stays above the line of the footer", diagram.page()).isLessThanOrEqualTo(footerLine);
            assertThat(diagram.height()).as("The diagram on page %d fills the page", diagram.page()).isGreaterThan(footerLine * 0.6f);
        }
        assertThat(diagrams.get(1).page()).as("The diagram of the table has a page of its own").isGreaterThan(diagrams.get(0).page());
        assertThat(pagesWhichCarry(pdf, "Diagram 1")).as("The header of the table stands on the page of its row").containsExactly(diagrams.get(1).page());
        assertThat(pageCountOf(pdf)).as("The empty paragraph after the table takes no page of its own").isEqualTo(diagrams.get(1).page() + 1);
        assertFalse(differ, "The pages differ from the reference images");
    }

    /**
     * Where the line of the footer runs, in points from the top of the page: the bottom margin of 90 px the default CSS gives
     * the page every page is, which a document without page breaks is printed on whatever its paper size.
     */
    @SneakyThrows
    private static float footerLineOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(0).getMediaBox().getHeight() - BOTTOM_MARGIN_PX * PT_PER_PX;
        }
    }

    @SneakyThrows
    private static int pageCountOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    /** The pages whose text holds the given words, counted from zero. */
    @SneakyThrows
    private static @NotNull List<Integer> pagesWhichCarry(byte @NotNull [] pdf, @NotNull String words) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<Integer> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                if (stripper.getText(document).contains(words)) {
                    pages.add(page - 1);
                }
            }
            return pages;
        }
    }

    @SneakyThrows
    private static byte @NotNull [] readImageResource(@NotNull String name) {
        try (InputStream resource = PageSizesTest.class.getResourceAsStream("/weasyprint/img/" + name)) {
            return Objects.requireNonNull(resource, name).readAllBytes();
        }
    }

}
