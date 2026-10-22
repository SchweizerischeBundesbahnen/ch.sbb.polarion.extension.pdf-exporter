package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A diagram in a table column follows the horizontal alignment of its column, centered or flush right, as an image of
 * the column does in a browser: one which fits its column as it is, and one fit to page shortens to the height of a page.
 * <p>
 * The content box of the column is measured, not computed: a word set flush left above the diagram, in its cell, starts at its
 * left edge, and one set flush right ends at its right edge. The position of the diagram is the one the reader sees, the
 * region its drawing is painted in.
 * </p>
 */
class SvgAlignmentInTableTest extends BasePdfConverterTest {

    /** A diagram of its own 200x100 px, which fits its column. */
    private static final String SMALL_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
              <rect x="2" y="2" width="196" height="96" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
              <circle cx="60" cy="50" r="24" fill="#1a73e8"/>
            </svg>""";

    /** The word set flush left above the diagram, which starts where its column does. */
    private static final String LEFT_MARK = "Left";

    /** The word set flush right above the diagram, which ends where its column does. */
    private static final String RIGHT_MARK = "Right";

    /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
    private static final float PT_PER_PX = 0.75f;

    /** How far, in points, a position may lie from the one expected: a few pixels of rounding. */
    private static final float TOLERANCE_PT = 2f;

    enum Alignment {
        CENTER, RIGHT
    }

    enum Diagram {
        /** Fits its column as it is. */
        SMALL(SMALL_SVG.getBytes(StandardCharsets.UTF_8), 200),
        /** 81x1521 px, which fit to page shortens to the height of a page. */
        TALL(readImageResource("diagram_20251002-1153.37186.mxg.svg"), 81);

        private final String source;
        private final int ownWidthPx;

        Diagram(byte @NotNull [] svg, int ownWidthPx) {
            this.source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg);
            this.ownWidthPx = ownWidthPx;
        }
    }

    static Stream<Arguments> placements() {
        return Arrays.stream(Alignment.values()).flatMap(alignment -> Arrays.stream(Diagram.values()).map(diagram -> Arguments.of(alignment, diagram)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("placements")
    void alignsAnSvgAsItsColumnDoes(@NotNull Alignment alignment, @NotNull Diagram diagram) {
        byte[] pdf = export(alignment, diagram);
        writeReportPdf("alignsAnSvgAsItsColumnDoes_" + alignment + "_" + diagram, "generated", pdf);

        List<DrawnImages.Box> boxes = DrawnImages.boxesIn(pdf);
        assertThat(boxes).as("The column draws its diagram once").hasSize(1);
        DrawnImages.Box drawn = boxes.getFirst();
        float columnLeft = edgeOf(pdf, LEFT_MARK, false);
        float columnRight = edgeOf(pdf, RIGHT_MARK, true);
        float leftGap = drawn.left() - columnLeft;
        float rightGap = columnRight - (drawn.left() + drawn.width());
        String measured = "column %.1f-%.1f pt, diagram %.1f-%.1f pt (width %.1f), gaps %.1f / %.1f pt".formatted(
                columnLeft, columnRight, drawn.left(), drawn.left() + drawn.width(), drawn.width(), leftGap, rightGap);

        if (diagram == Diagram.TALL) {
            assertThat(drawn.width()).as("Fit to page shortens the diagram, and its width follows: %s", measured).isLessThan(diagram.ownWidthPx * PT_PER_PX);
        } else {
            assertThat(drawn.width()).as("The diagram fits its column as it is: %s", measured).isCloseTo(diagram.ownWidthPx * PT_PER_PX, within(TOLERANCE_PT));
        }
        assertThat(leftGap).as("The diagram stays inside its column on the left: %s", measured).isGreaterThan(-TOLERANCE_PT);
        assertThat(rightGap).as("The diagram stays inside its column on the right: %s", measured).isGreaterThan(-TOLERANCE_PT);
        if (alignment == Alignment.CENTER) {
            assertThat(leftGap).as("The diagram stands in the middle of its column: %s", measured).isCloseTo(rightGap, within(TOLERANCE_PT));
        } else if (diagram == Diagram.TALL && !drawnByWeasysvg(pdf)) {
            // Known defect of the chromium SVG renderer: SvgProcessor gives the image the width of the SVG, and object-fit: contain
            // centers the shortened drawing in that wider box, short of the right edge by half the width it lost (SchweizerischeBundesbahnen/weasyprint-service#409)
            float shortOfTheEdge = (diagram.ownWidthPx * PT_PER_PX - drawn.width()) / 2;
            assertThat(rightGap).as("Chromium draws the diagram in the middle of a box as wide as the SVG, which stands flush right: %s", measured)
                    .isCloseTo(shortOfTheEdge, within(TOLERANCE_PT));
        } else {
            assertThat(rightGap).as("The diagram stands flush right in its column: %s", measured).isCloseTo(0f, within(TOLERANCE_PT));
        }

        // The shared reference of RIGHT TALL is the page Chromium draws, short of the right edge, and png/weasysvg holds the right one.
        // Known defect of the chromium SVG renderer: the shortened drawing is centered in a box as wide as the SVG (SchweizerischeBundesbahnen/weasyprint-service#409)
        assertFalse(compareContentUsingReferenceImages("alignsAnSvgAsItsColumnDoes_" + alignment + "_" + diagram, pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] export(@NotNull Alignment alignment, @NotNull Diagram diagram) {
        String align = alignment.name().toLowerCase();
        String html = """
                <table style="width: 100%%;">
                  <tr><td style="text-align: %3$s;">
                    <div style="text-align: left;">%1$s</div>
                    <div style="text-align: right;">%2$s</div>
                    <img src="%4$s" style="max-width: 650px;"/>
                  </td></tr>
                </table>""".formatted(LEFT_MARK, RIGHT_MARK, align, diagram.source);

        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .fitToPage(true)
                .build();
        return exportLiveDoc("SVG alignment", html, params);
    }

    /**
     * Where the line of the given word starts, or ends, in points from the left edge of the page: the origin of its first
     * glyph, or that of its last plus the advance of it, which are where the layout set the line, whatever the glyphs draw.
     */
    @SneakyThrows
    private static float edgeOf(byte @NotNull [] pdf, @NotNull String word, boolean end) {
        List<Float> edges = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (edges.isEmpty() && text.trim().equals(word)) {
                        TextPosition glyph = end ? positions.getLast() : positions.getFirst();
                        edges.add(end ? glyph.getXDirAdj() + glyph.getWidthDirAdj() : glyph.getXDirAdj());
                    }
                }
            };
            stripper.getText(document);
        }
        assertThat(edges).as("The column shows '%s'", word).isNotEmpty();
        return edges.getFirst();
    }

    @SneakyThrows
    private static byte @NotNull [] readImageResource(@NotNull String name) {
        try (InputStream resource = SvgAlignmentInTableTest.class.getResourceAsStream("/weasyprint/img/" + name)) {
            return Objects.requireNonNull(resource, name).readAllBytes();
        }
    }

    @SneakyThrows
    private static boolean drawnByWeasysvg(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return isDrawnByWeasysvg(document);
        }
    }
}
