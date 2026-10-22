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
import org.junit.jupiter.params.provider.EnumSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A diagram in a table column follows the horizontal alignment of its column, flush left, centered or flush right, as an
 * image of the column does in a browser: one which fits its column as it is, and one fit to page shortens to the height
 * of a page. A table of three columns shows the three alignments side by side.
 * <p>
 * The content box of each column is measured, not computed: a word set flush left above the diagram, in its cell, starts at
 * its left edge, and one set flush right ends at its right edge. The position of the diagram is the one the reader sees, the
 * region its drawing is painted in.
 * </p>
 */
class SvgAlignmentInTableTest extends BasePdfConverterTest {

    /** A diagram of its own 160x80 px, which fits its column. */
    private static final String SMALL_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="160" height="80" viewBox="0 0 200 100">
              <rect x="2" y="2" width="196" height="96" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
              <circle cx="60" cy="50" r="24" fill="#1a73e8"/>
            </svg>""";

    /** The style of each cell: a border, so the columns show on the page. */
    private static final String CELL = "vertical-align: top;border: 1px solid #CCCCCC;padding: 5px;width: 33%;";

    /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
    private static final float PT_PER_PX = 0.75f;

    /** How far, in points, a position may lie from the one expected: a few pixels of rounding. */
    private static final float TOLERANCE_PT = 2f;

    enum Alignment {
        LEFT, CENTER, RIGHT;

        /** The word set flush left above the diagram of this column, which starts where the column does. */
        String startMark() {
            return "Start" + ordinal();
        }

        /** The word set flush right above the diagram of this column, which ends where the column does. */
        String endMark() {
            return "End" + ordinal();
        }
    }

    enum Diagram {
        /** Fits its column as it is. */
        SMALL(SMALL_SVG.getBytes(StandardCharsets.UTF_8), 160),
        /** 81x1521 px, which fit to page shortens to the height of a page. */
        TALL(readImageResource("diagram_20251002-1153.37186.mxg.svg"), 81);

        private final String source;
        private final int ownWidthPx;

        Diagram(byte @NotNull [] svg, int ownWidthPx) {
            this.source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg);
            this.ownWidthPx = ownWidthPx;
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Diagram.class)
    void alignsAnSvgAsItsColumnDoes(@NotNull Diagram diagram) {
        byte[] pdf = export(diagram);
        String testName = "alignsAnSvgAsItsColumnDoes_" + diagram;
        writeReportPdf(testName, "generated", pdf);

        List<DrawnImages.Box> boxes = DrawnImages.boxesIn(pdf).stream().sorted(Comparator.comparing(DrawnImages.Box::left)).toList();
        assertThat(boxes).as("Each column draws its diagram once").hasSize(Alignment.values().length);
        for (Alignment alignment : Alignment.values()) {
            DrawnImages.Box drawn = boxes.get(alignment.ordinal());
            float columnLeft = edgeOf(pdf, alignment.startMark(), false);
            float columnRight = edgeOf(pdf, alignment.endMark(), true);
            float leftGap = drawn.left() - columnLeft;
            float rightGap = columnRight - (drawn.left() + drawn.width());
            String measured = "%s column %.1f-%.1f pt, diagram %.1f-%.1f pt (width %.1f), gaps %.1f / %.1f pt".formatted(
                    alignment, columnLeft, columnRight, drawn.left(), drawn.left() + drawn.width(), drawn.width(), leftGap, rightGap);

            if (diagram == Diagram.TALL) {
                assertThat(drawn.width()).as("Fit to page shortens the diagram, and its width follows: %s", measured).isLessThan(diagram.ownWidthPx * PT_PER_PX);
            } else {
                assertThat(drawn.width()).as("The diagram fits its column as it is: %s", measured).isCloseTo(diagram.ownWidthPx * PT_PER_PX, within(TOLERANCE_PT));
            }
            assertThat(leftGap).as("The diagram stays inside its column on the left: %s", measured).isGreaterThan(-TOLERANCE_PT);
            assertThat(rightGap).as("The diagram stays inside its column on the right: %s", measured).isGreaterThan(-TOLERANCE_PT);

            switch (alignment) {
                case LEFT -> assertThat(leftGap).as("The diagram stands flush left in its column: %s", measured).isCloseTo(0f, within(TOLERANCE_PT));
                case CENTER -> assertThat(leftGap).as("The diagram stands in the middle of its column: %s", measured).isCloseTo(rightGap, within(TOLERANCE_PT));
                case RIGHT -> assertThat(rightGap).as("The diagram stands flush right in its column: %s", measured).isCloseTo(0f, within(TOLERANCE_PT));
            }
        }

        assertFalse(compareContentUsingReferenceImages(testName, pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] export(@NotNull Diagram diagram) {
        StringBuilder header = new StringBuilder();
        StringBuilder row = new StringBuilder();
        for (Alignment alignment : Alignment.values()) {
            String align = alignment.name().toLowerCase();
            header.append("<th style=\"%stext-align: %s;background-color: #F0F0F0;\">%s</th>".formatted(CELL, align, align));
            row.append("""
                    <td style="%stext-align: %s;">
                      <div style="text-align: left;">%s</div>
                      <div style="text-align: right;">%s</div>
                      <img src="%s" style="max-width: 650px;"/>
                    </td>""".formatted(CELL, align, alignment.startMark(), alignment.endMark(), diagram.source));
        }
        String html = """
                <table style="width: 100%%;table-layout: fixed;border: 1px solid #CCCCCC;border-collapse: collapse;">
                  <tr>%s</tr>
                  <tr>%s</tr>
                </table>""".formatted(header, row);

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
}
