package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Visio drawing attached to a document arrives in the PDF as a drawing.
 * <p>
 * The export inlines a {@code .vsdx} attachment with the type it guesses from its name, and weasyprint-service draws
 * it through weasyvsdx and libvisio as vector, with its text as text. The drawing is {@code test-data/diagram/test.vsdx}
 * of <a href="https://github.com/apache/poi">Apache POI</a>, Apache License 2.0: a container with a blue header bar,
 * two lines of text and a star.
 * </p>
 */
class VsdxImageTest extends BasePdfConverterTest {

    /** The type an export gives a {@code .vsdx} attachment, see {@code MediaUtilsTest}, and weasyprint-service draws a Visio drawing by. */
    private static final String VSDX_TYPE = "application/vnd.ms-visio.drawing";

    @Test
    @SneakyThrows
    void drawsAVsdxAttachmentAsVector() {
        byte[] drawing = IOUtils.resourceToByteArray("/weasyprint/img/apache-poi-test.vsdx");
        String html = """
                <p>Before the drawing</p>
                <p><img src="data:%s;base64,%s" style="max-width: 650px;"/></p>
                <p>After the drawing</p>""".formatted(VSDX_TYPE, Base64.getEncoder().encodeToString(drawing));

        byte[] pdf = export(html);

        assertEquals(List.of(), rasterImagesIn(pdf), "The drawing stays vector, it is not rasterized to a picture");
        String text = textOf(pdf);
        assertTrue(text.contains("This is a test."), "The text of the drawing is text in the PDF: " + text);
        assertTrue(text.contains("Nothing fancy."), "The text of the drawing is text in the PDF: " + text);
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] export(@NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        return exportLiveDoc("Visio drawing", content, params);
    }

    /** The names of the image XObjects the pages draw directly. */
    @SneakyThrows
    private static @NotNull List<String> rasterImagesIn(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> images = new ArrayList<>();
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                for (COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        images.add(name.getName());
                    }
                }
            }
            return images;
        }
    }

    @SneakyThrows
    private static @NotNull String textOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }
}
