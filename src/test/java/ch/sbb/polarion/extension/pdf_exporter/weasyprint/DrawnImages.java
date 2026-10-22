package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The size every image of a pdf is drawn at, which is what a reader of the page sees.
 * <p>
 * WeasyPrint service draws an SVG in one of two ways, as its {@code SVG_RENDERER} states. Chromium rasterizes it to a
 * PNG, an image XObject like any other picture. The weasysvg plugin draws it as vector, a form XObject of its own
 * without a transparency group: the forms WeasyPrint itself makes, for a page or for opacity, all carry one. Both
 * count as an image here, so a test checks the same number, size and place of its images whichever renderer drew them.
 * A form counted as an image is not read further, so a picture inside an SVG is not counted a second time.
 * </p>
 */
final class DrawnImages {

    /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
    private static final float PT_PER_PX = 0.75f;

    /** The square an image XObject is drawn into, before the matrix in force scales it to its size. */
    private static final PDRectangle UNIT_SQUARE = new PDRectangle(0, 0, 1, 1);

    private DrawnImages() {
    }

    /** The size of every image drawn in the document, in CSS pixels, in the order they are drawn. */
    static @NotNull List<List<Integer>> sizesIn(byte @NotNull [] pdf) {
        return boxesIn(pdf).stream()
                .map(box -> size(Math.round(box.width() / PT_PER_PX), Math.round(box.height() / PT_PER_PX)))
                .toList();
    }

    static @NotNull List<Integer> size(int width, int height) {
        return List.of(width, height);
    }

    /**
     * Where an image is drawn: the page counted from zero, and its box in points, measured from the top left corner
     * of the page as text positions are.
     */
    record Box(int page, float left, float top, float width, float height) {
        float middle() {
            return top + height / 2;
        }
    }

    /** The box of every image drawn in the document, in the order they are drawn. */
    @SneakyThrows
    static @NotNull List<Box> boxesIn(byte @NotNull [] pdf) {
        List<Box> boxes = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                PDPage page = document.getPage(index);
                new Collector(page, index, boxes).processPage(page);
            }
        }
        return boxes;
    }

    /** Reads the box each image is drawn in: the rectangle it is drawn into, through the matrix in force. */
    private static class Collector extends PDFGraphicsStreamEngine {

        private final int pageIndex;
        private final float pageHeight;
        private final List<Box> boxes;

        Collector(@NotNull PDPage page, int pageIndex, @NotNull List<Box> boxes) {
            super(page);
            this.pageIndex = pageIndex;
            this.pageHeight = page.getMediaBox().getHeight();
            this.boxes = boxes;
        }

        @Override
        public void drawImage(@NotNull PDImage image) {
            add(UNIT_SQUARE, getGraphicsState().getCurrentTransformationMatrix());
        }

        /**
         * A form without a transparency group is an SVG the weasysvg plugin drew. A transparency group goes to
         * {@link #showTransparencyGroup} instead, which reads it as part of the page.
         */
        @Override
        public void showForm(@NotNull PDFormXObject form) {
            add(form.getBBox(), form.getMatrix().multiply(getGraphicsState().getCurrentTransformationMatrix()));
        }

        private void add(@NotNull PDRectangle rectangle, @NotNull Matrix matrix) {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float[][] corners = {
                    {rectangle.getLowerLeftX(), rectangle.getLowerLeftY()},
                    {rectangle.getUpperRightX(), rectangle.getLowerLeftY()},
                    {rectangle.getLowerLeftX(), rectangle.getUpperRightY()},
                    {rectangle.getUpperRightX(), rectangle.getUpperRightY()},
            };
            for (float[] corner : corners) {
                Point2D.Float point = matrix.transformPoint(corner[0], corner[1]);
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }
            boxes.add(new Box(pageIndex, minX, pageHeight - maxY, maxX - minX, maxY - minY));
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
