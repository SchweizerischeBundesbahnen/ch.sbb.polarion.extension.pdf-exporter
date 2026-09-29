package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.PageWidthAdjuster;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The size an image is given in the document is the size it has in the PDF.
 * <p>
 * A Polarion diagram is an SVG attachment, and the size its author chose travels as an inline style on the
 * image. Converting the SVG to PNG used to drop that style, so every diagram came out at its own size -
 * reported in #1077 and fixed in weasyprint-service (#375).
 * </p>
 */
class ImageSizeTest extends BaseWeasyPrintTest {

    /** A diagram of its own 200x100 px, drawn so that its edges are visible in a rendered page. */
    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
              <rect x="2" y="2" width="196" height="96" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
              <circle cx="60" cy="50" r="24" fill="#1a73e8"/>
            </svg>""";

    /** A diagram of its own 81x1521 px, which is taller than a page. */
    private static final String TALL_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="81" height="1521" viewBox="0 0 81 1521">
              <rect x="2" y="2" width="77" height="1517" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
            </svg>""";

    /** The height of a portrait A4 page, which is what fit to page allows an image. */
    private static final int PAGE_HEIGHT = 874;

    private static final int OWN_WIDTH = 200;
    private static final int OWN_HEIGHT = 100;

    @Test
    @SneakyThrows
    void keepsTheSizeTheDocumentGivesAnSvg() {
        String source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(SVG.getBytes());
        String html = """
                <html><body>
                  <p><img src="%1$s" style="max-width: 650px;"/></p>
                  <p><img src="%1$s" style="max-width: 650px; width: 100px;"/></p>
                  <p><img src="%1$s" style="max-width: 650px; width: 400px;"/></p>
                  <p><img src="%1$s" style="width: 300px; height: 150px;"/></p>
                </body></html>""".formatted(source);

        byte[] pdf = exportToPdf(html, WeasyPrintOptions.builder().build());

        // its own size where the document gives none, then half of it, twice it, and a width with a height
        assertEquals(List.of(size(OWN_WIDTH, OWN_HEIGHT), size(100, 50), size(400, 200), size(300, 150)), imageSizesInPx(pdf));
    }

    @Test
    @SneakyThrows
    void keepsTheRatioOfADiagramWhichIsTallerThanThePage() {
        String source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(TALL_SVG.getBytes());
        String html = """
                <html><body><p><img src="%s" style="max-width: 650px;"/></p></body></html>""".formatted(source);

        String adjusted = new PageWidthAdjuster(html, ConversionParams.builder().build())
                .adjustImageSizeInTables()
                .adjustImageSize()
                .adjustTableSize()
                .toHTML();

        byte[] pdf = exportToPdf("<html><body>%s</body></html>".formatted(adjusted), WeasyPrintOptions.builder().build());

        // Fit to page shortens it to the height of a page, and the width follows: 81 * 874 / 1521
        assertEquals(List.of(size(47, PAGE_HEIGHT)), imageSizesInPx(pdf));
    }

    private static @NotNull List<Integer> size(int width, int height) {
        return List.of(width, height);
    }

    /** The size of every image drawn in the document, in CSS pixels, in the order they are drawn. */
    @SneakyThrows
    private @NotNull List<List<Integer>> imageSizesInPx(byte @NotNull [] pdf) {
        List<List<Integer>> sizes = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (PDPage page : document.getPages()) {
                new ImageSizeCollector(page, sizes).processPage(page);
            }
        }
        return sizes;
    }

    /** Reads the size each image is drawn at, which is the scale of the matrix in force. */
    private static class ImageSizeCollector extends PDFGraphicsStreamEngine {

        /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
        private static final float PT_PER_PX = 0.75f;

        private final List<List<Integer>> sizes;

        ImageSizeCollector(@NotNull PDPage page, @NotNull List<List<Integer>> sizes) {
            super(page);
            this.sizes = sizes;
        }

        @Override
        public void drawImage(@NotNull PDImage image) {
            Matrix matrix = getGraphicsState().getCurrentTransformationMatrix();
            sizes.add(size(Math.round(matrix.getScalingFactorX() / PT_PER_PX), Math.round(matrix.getScalingFactorY() / PT_PER_PX)));
        }

        // The rest of the drawing operations say nothing about an image, so they are read and dropped.

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            // see above
        }

        @Override
        public void clip(int windingRule) {
            // see above
        }

        @Override
        public void moveTo(float x, float y) {
            // see above
        }

        @Override
        public void lineTo(float x, float y) {
            // see above
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            // see above
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float();
        }

        @Override
        public void closePath() {
            // see above
        }

        @Override
        public void endPath() {
            // see above
        }

        @Override
        public void strokePath() {
            // see above
        }

        @Override
        public void fillPath(int windingRule) {
            // see above
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
            // see above
        }

        @Override
        public void shadingFill(COSName shadingName) {
            // see above
        }
    }
}
