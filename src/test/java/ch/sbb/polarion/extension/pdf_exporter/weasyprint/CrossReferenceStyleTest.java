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
 * removes the link around the label, and the label keeps its look. A link to a Polarion object with a label of its own
 * looks the same.
 * </p>
 */
class CrossReferenceStyleTest extends BasePdfConverterTest {

    /** No letter of the labels reaches below the baseline, where the dashed line runs. */
    private static final String LABEL = "EL-1 - Item 1";

    /** A cross-reference as Polarion renders it, with a link "Cut local Polarion URLs" removes. */
    private static final String CONTENT = """
            <div class="polarion-dle-workitem-basic-0">to Item 1 : <span class="polarion-rte-link" data-type="crossReference" data-item-id="EL-1" data-option-id="long"><span class="polarion-no-style-cleanup" style="white-space:nowrap;" title="EL-1 - Item 1 (Scroll in document)"><a style="font-size:1em;" target="_top" class="polarion-Hyperlink" href="http://localhost/polarion/#/project/test/workitem?id=EL-1"><span class="polarion-cross-reference-label">EL-1<span style="white-space: normal"> - Item 1</span></span></a></span></span></div>""";

    private static final String EXTERNAL_LABEL = "Item 2";

    /** A link to a Polarion object with a label of its own, as Polarion renders it: the link carries the class. */
    private static final String EXTERNAL = """
            <div class="polarion-dle-workitem-basic-0">to the second item : <span class="polarion-rte-link" data-type="polarion"><a href="http://localhost/polarion/#/project/test/workitem?id=EL-2" class="polarion-cross-reference-link-external">%s</a></span></div>"""
            .formatted(EXTERNAL_LABEL);

    /** The color of a link in Polarion and in the default CSS. */
    private static final Color LINK = new Color(0x007993);

    /** The color of the dashed line below a cross-reference in Polarion. */
    private static final Color LINE = new Color(0xA9A9A9);

    /** How far an antialiased pixel may be from the color it draws, per channel. */
    private static final int TOLERANCE = 40;

    /** How far the channels of a gray pixel may be from each other. */
    private static final int NEUTRAL = 8;

    /** The pages are rendered at 300 DPI, a point is 300 / 72 pixels. */
    private static final float PIXELS_PER_POINT = 300f / 72f;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void drawsACrossReferenceAsPolarionDoes(boolean cutLocalUrls) {
        assertDrawnAsPolarionDoes(export(CONTENT, cutLocalUrls), LABEL);
    }

    @Test
    void drawsALinkWithALabelOfItsOwnAsPolarionDoes() {
        assertDrawnAsPolarionDoes(export(EXTERNAL, false), EXTERNAL_LABEL);
    }

    private static void assertDrawnAsPolarionDoes(byte @NotNull [] pdf, @NotNull String text) {
        BufferedImage page = pageOf(pdf);
        Rectangle label = boxOf(pdf, text);
        assertThat(pixels(page, label, LINK)).as("The label is in the color of a link").isPositive();
        assertThat(pixels(page, label, Color.BLACK)).as("The label is not black").isZero();

        Rectangle below = new Rectangle(label.x, label.y + label.height, label.width, Math.round(4 * PIXELS_PER_POINT));
        assertThat(pixels(page, below, Color.BLACK)).as("The label is not underlined in black").isZero();
        assertThat(dashesIn(page, below)).as("A dashed gray line runs below the label").isGreaterThan(3);
    }

    @Test
    void drawsACrossReferenceAsTheReferenceImageShows() {
        byte[] pdf = export(CONTENT, false);

        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    void drawsACrossReferenceWithoutItsLinkAsWithIt() {
        BufferedImage linked = pageOf(export(CONTENT, false));
        BufferedImage cut = pageOf(export(CONTENT, true));

        assertThat(MediaUtils.diffImages(linked, cut)).as("Cutting local URLs leaves the page as it is").isEmpty();
    }

    private byte @NotNull [] export(@NotNull String content, boolean cutLocalUrls) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .cutLocalUrls(cutLocalUrls)
                .build();
        return exportLiveDoc("Cross-references", content, params);
    }

    @SneakyThrows
    private static @NotNull BufferedImage pageOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return MediaUtils.pdfPageToImage(document, 0);
        }
    }

    /** The box of a label on the first page, in pixels, from the top of its letters to its baseline. */
    @SneakyThrows
    private static @NotNull Rectangle boxOf(byte @NotNull [] pdf, @NotNull String label) {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    int start = text.indexOf(label);
                    if (start >= 0) {
                        letters.addAll(positions.subList(start, start + label.length()));
                    }
                }
            }.getText(document);
        }
        assertThat(letters).as("The label is in the PDF").hasSize(label.length());
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
                boolean gray = isGray(new Color(page.getRGB(x, y)));
                if (gray && !inDash) {
                    dashes++;
                }
                inDash = gray;
            }
            most = Math.max(most, dashes);
        }
        return most;
    }

    /** The color of the line, and neutral: the edges of a letter in the color of a link are not gray. */
    private static boolean isGray(@NotNull Color color) {
        return isClose(color, LINE)
                && Math.abs(color.getRed() - color.getGreen()) <= NEUTRAL
                && Math.abs(color.getGreen() - color.getBlue()) <= NEUTRAL;
    }

    private static boolean isClose(@NotNull Color actual, @NotNull Color expected) {
        return Math.abs(actual.getRed() - expected.getRed()) <= TOLERANCE
                && Math.abs(actual.getGreen() - expected.getGreen()) <= TOLERANCE
                && Math.abs(actual.getBlue() - expected.getBlue()) <= TOLERANCE;
    }
}
