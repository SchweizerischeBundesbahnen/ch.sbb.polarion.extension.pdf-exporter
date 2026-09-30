package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.util.Matrix;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** The size every image of a pdf is drawn at, which is what a reader of the page sees. */
final class DrawnImages {

    /** A CSS pixel is 0.75 pt, the unit a PDF is laid out in. */
    private static final float PT_PER_PX = 0.75f;

    private DrawnImages() {
    }

    /** The size of every image drawn in the document, in CSS pixels, in the order they are drawn. */
    @SneakyThrows
    static @NotNull List<List<Integer>> sizesIn(byte @NotNull [] pdf) {
        List<List<Integer>> sizes = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (PDPage page : document.getPages()) {
                new Collector(page, sizes).processPage(page);
            }
        }
        return sizes;
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
                int pageIndex = index;
                float pageHeight = page.getMediaBox().getHeight();
                new Collector(page, new ArrayList<>()) {
                    @Override
                    public void drawImage(@NotNull PDImage image) {
                        Matrix matrix = getGraphicsState().getCurrentTransformationMatrix();
                        float height = matrix.getScalingFactorY();
                        boxes.add(new Box(pageIndex, matrix.getTranslateX(), pageHeight - matrix.getTranslateY() - height, matrix.getScalingFactorX(), height));
                    }
                }.processPage(page);
            }
        }
        return boxes;
    }

    /** Reads the size each image is drawn at, which is the scale of the matrix in force. */
    private static class Collector extends PDFGraphicsStreamEngine {

        private final List<List<Integer>> sizes;

        Collector(@NotNull PDPage page, @NotNull List<List<Integer>> sizes) {
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
