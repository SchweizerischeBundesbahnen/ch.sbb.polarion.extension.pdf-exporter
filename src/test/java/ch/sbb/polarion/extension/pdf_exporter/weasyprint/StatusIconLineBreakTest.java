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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The icon of an enum value stays on the line of its label.
 * <p>
 * A work item lists its fields after its description, in brackets: {@code [<icon>Draft]}. After a wide image, such as
 * a Visio drawing, the line had room for the bracket and the icon alone, and the label went to the next line, as a
 * line may break after an image. The icon and its label now move to the next line together.
 * </p>
 */
class StatusIconLineBreakTest extends BasePdfConverterTest {

    /** A square icon of 16 pixels, as Polarion draws the icon of a status. */
    private static final String ICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAF0lEQVR4nGM4w8BAEiJN9aiGUQ1DSgMAQWfMAdovJBMAAAAASUVORK5CYII=";

    /** The widths of the image before the fields, in pixels: from a line with room to spare to one with none, past the width of the column. */
    private static final List<Integer> IMAGE_WIDTHS = IntStream.rangeClosed(0, 40).map(step -> 520 + step * 4).boxed().toList();

    /** An icon is 12 pt, the images before the fields are far wider. */
    private static final float MAX_ICON_WIDTH_PT = 20f;

    /** How far the middle of an icon may be from the middle of its label: the icon is drawn by whole pixels. */
    private static final float TOLERANCE_PT = 3f;

    @Test
    void keepsTheIconOfAStatusOnTheLineOfItsLabel() {
        String html = IMAGE_WIDTHS.stream().map(StatusIconLineBreakTest::workItem).collect(Collectors.joining("\n"));

        byte[] pdf = export(html);

        List<DrawnImages.Box> icons = DrawnImages.boxesIn(pdf).stream().filter(box -> box.width() < MAX_ICON_WIDTH_PT).toList();
        List<TextPosition> labels = labelsOf(pdf);
        assertThat(icons).hasSameSizeAs(IMAGE_WIDTHS);
        assertThat(labels).hasSameSizeAs(IMAGE_WIDTHS);
        for (int item = 0; item < icons.size(); item++) {
            DrawnImages.Box icon = icons.get(item);
            TextPosition label = labels.get(item);
            float labelMiddle = label.getYDirAdj() - label.getHeightDir() / 2;
            assertThat(icon.middle())
                    .as("The icon after an image of %d px is on the line of its label", IMAGE_WIDTHS.get(item))
                    .isCloseTo(labelMiddle, within(TOLERANCE_PT));
            assertThat(label.getXDirAdj())
                    .as("The label after an image of %d px follows its icon", IMAGE_WIDTHS.get(item))
                    .isGreaterThan(icon.left());
        }
    }

    /** A work item as Polarion writes it: a description ending in an image, and its status in brackets. */
    private static @NotNull String workItem(int imageWidth) {
        return """
                <div class="polarion-dle-workitem-basic-0">test<br/><img src="%1$s" style="width: %2$dpx; height: 40px;"/><span class="polarion-dle-workitem-fields-end"><span class="polarion-dle-workitem-fields-end-inner">&thinsp;<b>[</b><span id="polarion_editor_field=status"><span class="polarion-JSEnumOption" title="Draft"><img style="vertical-align:bottom;border:0px;margin-right:2px;" src="%1$s" alt=""/>Draft</span></span>&thinsp;<b>]</b></span></span></div>"""
                .formatted(ICON, imageWidth);
    }

    private byte @NotNull [] export(@NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        return exportLiveDoc("Status icons", content, params);
    }

    /** The first letter of each label, in the order of the document. */
    @SneakyThrows
    private static @NotNull List<TextPosition> labelsOf(byte @NotNull [] pdf) {
        List<TextPosition> labels = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    int start = text.indexOf("Draft");
                    if (start >= 0) {
                        labels.add(positions.get(start));
                    }
                }
            }.getText(document);
        }
        return labels;
    }
}
