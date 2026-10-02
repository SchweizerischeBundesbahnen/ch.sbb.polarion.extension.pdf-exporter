package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.PaperSizeUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The size an image is given in the document is the size it has in the PDF.
 * <p>
 * A Polarion diagram is an SVG attachment, and the size its author chose travels as an inline style on the
 * image. Converting the SVG to PNG used to drop that style, so every diagram came out at its own size -
 * reported in #1077 and fixed in weasyprint-service (#375).
 * </p>
 */
class ImageSizeTest extends BasePdfConverterTest {

    /** A diagram of its own 200x100 px, drawn so that its edges are visible in a rendered page. */
    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
              <rect x="2" y="2" width="196" height="96" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
              <circle cx="60" cy="50" r="24" fill="#1a73e8"/>
            </svg>""";

    /** A diagram of the system test's own document, 81x1521 px, taller than a page. */
    private static final String TALL_SVG = readImageResource("diagram_20251002-1153.37186.mxg.svg");

    /** The pages the document of the fit to page system test runs to. */
    private static final int DOCUMENT_PAGES = 7;

    /** The height of a portrait A4 page, which is what fit to page allows an image. */
    private static final int PAGE_HEIGHT = 874;

    private static final int OWN_WIDTH = 200;
    private static final int OWN_HEIGHT = 100;

    @Test
    @SneakyThrows
    void keepsTheSizeTheDocumentGivesAnSvg() {
        String source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(SVG.getBytes());
        String html = """
                <p><img src="%1$s" style="max-width: 650px;"/></p>
                <p><img src="%1$s" style="max-width: 650px; width: 100px;"/></p>
                <p><img src="%1$s" style="max-width: 650px; width: 400px;"/></p>
                <p><img src="%1$s" style="width: 300px; height: 150px;"/></p>""".formatted(source);

        byte[] pdf = export(html, false);

        // its own size where the document gives none, then half of it, twice it, and a width with a height
        assertEquals(List.of(DrawnImages.size(OWN_WIDTH, OWN_HEIGHT), DrawnImages.size(100, 50), DrawnImages.size(400, 200), DrawnImages.size(300, 150)), DrawnImages.sizesIn(pdf));
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    @SneakyThrows
    void keepsTheRatioOfADiagramWhichIsTallerThanThePage() {
        String source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(TALL_SVG.getBytes());
        String html = """
                <p><img src="%s" style="max-width: 650px;"/></p>""".formatted(source);

        byte[] pdf = export(html, true);

        // Fit to page shortens it to the height of a page, and the width follows: 81 * 874 / 1521
        assertEquals(List.of(DrawnImages.size(47, PAGE_HEIGHT)), DrawnImages.sizesIn(pdf));
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /** A page break may turn a block of a portrait document, and the diagram is fitted to the page it is printed on. */
    @Test
    @SneakyThrows
    void fitsADiagramToTheLandscapePageOfItsBlock() {
        String source = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(TALL_SVG.getBytes());
        String html = """
                <p><img src="%s" style="max-width: 650px;"/></p><!--PAGE_BREAK--><!--LANDSCAPE_ABOVE--><p>A portrait page</p>""".formatted(source);

        byte[] pdf = export(html, true);

        List<List<Integer>> sizes = DrawnImages.sizesIn(pdf);
        assertEquals(1, sizes.size(), "The document holds one image");
        int landscapeHeight = PaperSizeUtils.MAX_LANDSCAPE_HEIGHTS.get(PaperSize.A4);
        assertTrue(sizes.getFirst().getLast() <= landscapeHeight, "The diagram is no taller than the landscape page it is printed on");
        assertEquals(81d / 1521d, (double) sizes.getFirst().getFirst() / sizes.getFirst().getLast(), 0.01d, "The diagram keeps its own shape");
        assertEquals(2, pageCount(pdf), "The diagram fits its landscape page, and the text after the page break takes a portrait one");
    }

    @Test
    @SneakyThrows
    void keepsTheRatioOfAnImageFittedToItsColumn() {
        String html = """
                <table>
                  <tr>
                    <td><img src="%s" style="width: 800px; height: 300px;"/></td>
                    <td>A cell which leaves the image a column narrower than the width it states</td>
                  </tr>
                </table>""".formatted(rasterSource());

        byte[] pdf = export(html, true);

        List<List<Integer>> sizes = DrawnImages.sizesIn(pdf);
        assertEquals(1, sizes.size(), "The document holds one image");

        int width = sizes.getFirst().getFirst();
        int height = sizes.getFirst().getLast();
        assertTrue(width < 800, "The column is narrower than the width the image states, so it limits the image");
        // The image keeps its own shape, 100 / 200: a height stated for the width it no longer has would distort it
        assertEquals(100d / 200d, (double) height / width, 0.02d);
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /**
     * The document of the fit to page system test, page by page. It holds the shapes a document states for an
     * image: a size larger than a column, a diagram taller than a page, a wider one, and a height a drag of a
     * handle in the editor left behind.
     */
    @Test
    @SneakyThrows
    void fitsTheImagesOfADocumentToThePage() {
        String html = withAttachments(readHtmlResource("imagesTest"));

        byte[] pdf = export(html, true);

        assertEquals(DOCUMENT_PAGES, pageCount(pdf), "The document runs to a page count of its own, and a page which never arrives is compared with nothing");
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /** Exports the content as a document, fitted to the page or not. */
    private byte @NotNull [] export(@NotNull String content, boolean fitToPage) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .fitToPage(fitToPage)
                .build();
        return exportLiveDoc("Images", content, params);
    }

    /** Reads every attachment the document names out of the test resources, as an export reads them from Polarion. */
    private @NotNull String withAttachments(@NotNull String html) {
        Matcher attachments = Pattern.compile("attachment:([^\"]+)").matcher(html);
        StringBuilder result = new StringBuilder();
        while (attachments.find()) {
            String name = attachments.group(1);
            String type = name.endsWith(".svg") ? "image/svg+xml" : "image/jpeg";
            String source = "data:%s;base64,%s".formatted(type, Base64.getEncoder().encodeToString(readImageResource(name).getBytes(StandardCharsets.ISO_8859_1)));
            attachments.appendReplacement(result, Matcher.quoteReplacement(source));
        }
        attachments.appendTail(result);
        return result.toString();
    }

    @SneakyThrows
    private static @NotNull String readImageResource(@NotNull String name) {
        try (InputStream resource = ImageSizeTest.class.getResourceAsStream("/weasyprint/img/" + name)) {
            return new String(Objects.requireNonNull(resource, name).readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    /** The same diagram as a raster: unlike an SVG, its drawing follows the box the document gives it. */
    @SneakyThrows
    private static @NotNull String rasterSource() {
        BufferedImage image = new BufferedImage(OWN_WIDTH, OWN_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, OWN_WIDTH, OWN_HEIGHT);
            graphics.setStroke(new BasicStroke(3));
            for (int i = 0; i < 2; i++) {
                // A box is kept clear of the edge, where the raster would cut its stroke in half
                int x = 10 + i * 100;
                graphics.setColor(new Color(0xe8f0fe));
                graphics.fillRect(x, 25, 80, 50);
                graphics.setColor(new Color(0x1a73e8));
                graphics.drawRect(x, 25, 80, 50);
            }
            graphics.drawLine(90, 50, 110, 50);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    }

    @SneakyThrows
    private int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

}
