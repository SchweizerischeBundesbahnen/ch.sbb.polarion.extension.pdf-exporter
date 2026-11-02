package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A cross-reference looks as it does in Polarion: in the color of a link, with a dashed gray line below.
 * <p>
 * The default CSS drew it black and underlined, while every other link of the document is blue. Cutting local URLs
 * removes the link around the label, and the label keeps its look.
 * </p>
 */
class CrossReferenceStyleTest extends BasePdfConverterTest {

    private static final String LABEL = "1 - Object 1";

    private static final String CONTENT = """
            <div class="polarion-dle-workitem-basic-0">to Object 1 : <span class="polarion-rte-link" data-type="crossReference" data-item-id="EL-1" data-option-id="long"><a href="http://localhost/polarion/#/project/test/workitem?id=EL-1"><span class="polarion-cross-reference-label">%s</span></a></span></div>"""
            .formatted(LABEL);

    /** The color of a link in Polarion and in the default CSS. */
    private static final Color LINK = new Color(0x007993);

    /** The color of the dashed line below a cross-reference in Polarion. */
    private static final Color LINE = new Color(0xA9A9A9);

    /** How far an antialiased pixel may be from the color it draws, per channel. */
    private static final int TOLERANCE = 40;

    /** The pages are rendered at 300 DPI, a point is 300 / 72 pixels. */
    private static final float PIXELS_PER_POINT = 300f / 72f;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void drawsACrossReferenceAsPolarionDoes(boolean cutLocalUrls) {
        byte[] pdf = export(cutLocalUrls);

        BufferedImage page = pageOf(pdf);
        Rectangle label = boxOf(pdf);
        assertThat(pixels(page, label, LINK)).as("The label is in the color of a link").isPositive();
        assertThat(pixels(page, label, Color.BLACK)).as("The label is not black").isZero();

        Rectangle below = new Rectangle(label.x, label.y + label.height, label.width, Math.round(4 * PIXELS_PER_POINT));
        assertThat(pixels(page, below, Color.BLACK)).as("The label is not underlined in black").isZero();
        assertThat(dashesIn(page, below)).as("A dashed gray line runs below the label").isGreaterThan(3);
    }

    @Test
    void drawsACrossReferenceAsTheReferenceImageShows() {
        byte[] pdf = export(false);

        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    void drawsACrossReferenceWithoutItsLinkAsWithIt() {
        BufferedImage linked = pageOf(export(false));
        BufferedImage cut = pageOf(export(true));

        assertThat(MediaUtils.diffImages(linked, cut)).as("Cutting local URLs leaves the page as it is").isEmpty();
    }

    private byte @NotNull [] export(boolean cutLocalUrls) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .cutLocalUrls(cutLocalUrls)
                .build();
        return exportLiveDoc("Cross-references", CONTENT, params);
    }

    @SneakyThrows
    private static @NotNull BufferedImage pageOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return MediaUtils.pdfPageToImage(document, 0);
        }
    }

    /** The box of the label on the first page, in pixels, from the top of its letters to its baseline. */
    @SneakyThrows
    private static @NotNull Rectangle boxOf(byte @NotNull [] pdf) {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    int start = text.indexOf(LABEL);
                    if (start >= 0) {
                        letters.addAll(positions.subList(start, start + LABEL.length()));
                    }
                }
            }.getText(document);
        }
        assertThat(letters).as("The label is in the PDF").hasSize(LABEL.length());
        TextPosition first = letters.get(0);
        TextPosition last = letters.get(letters.size() - 1);
        int left = Math.round(first.getXDirAdj() * PIXELS_PER_POINT);
        int right = Math.round((last.getXDirAdj() + last.getWidthDirAdj()) * PIXELS_PER_POINT);
        int baseline = Math.round(first.getYDirAdj() * PIXELS_PER_POINT);
        int top = Math.round((first.getYDirAdj() - first.getHeightDir()) * PIXELS_PER_POINT);
        return new Rectangle(left, top, right - left, baseline - top);
    }

    private static int pixels(@NotNull BufferedImage page, @NotNull Rectangle box, @NotNull Color color) {
        int count = 0;
        for (int y = box.y; y < box.y + box.height; y++) {
            for (int x = box.x; x < box.x + box.width; x++) {
                if (isClose(new Color(page.getRGB(x, y)), color)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The most runs of gray pixels in one row of the box: a dashed line has several, a solid line one. */
    private static int dashesIn(@NotNull BufferedImage page, @NotNull Rectangle box) {
        int most = 0;
        for (int y = box.y; y < box.y + box.height; y++) {
            int dashes = 0;
            boolean inDash = false;
            for (int x = box.x; x < box.x + box.width; x++) {
                boolean gray = isClose(new Color(page.getRGB(x, y)), LINE);
                if (gray && !inDash) {
                    dashes++;
                }
                inDash = gray;
            }
            most = Math.max(most, dashes);
        }
        return most;
    }

    private static boolean isClose(@NotNull Color actual, @NotNull Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) <= TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) <= TOLERANCE;
    }
}
