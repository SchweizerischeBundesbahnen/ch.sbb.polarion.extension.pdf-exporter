package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Visio drawing in the binary format of Visio 2003 to 2010, VSD, arrives in the PDF as a drawing.
 * <p>
 * The export inlines a {@code .vsd} attachment with the type it guesses from its name, and weasyprint-service draws
 * it through weasyvsdx and libvisio as vector, with its text as text. The drawing is
 * {@code test-data/diagram/Test_Visio-Some_Random_Text.vsd} of <a href="https://github.com/apache/poi">Apache POI</a>,
 * Apache License 2.0: a box with a title and a line of text, and a line of text on the page.
 * </p>
 */
class VsdImageTest extends BasePdfConverterTest {

    /** The type an export gives a {@code .vsd} attachment, see {@code MediaUtilsTest}, and weasyprint-service draws a Visio drawing by. */
    private static final String VSD_TYPE = "application/vnd.visio";

    @Test
    @SneakyThrows
    void drawsAVsdAttachmentAsVector() {
        byte[] drawing = IOUtils.resourceToByteArray("/weasyprint/img/apache-poi-test.vsd");
        String html = """
                <p>Before the drawing</p>
                <p><img src="data:%s;base64,%s" style="max-width: 650px;"/></p>
                <p>After the drawing</p>""".formatted(VSD_TYPE, Base64.getEncoder().encodeToString(drawing));

        byte[] pdf = export(html);

        assertEquals(List.of(), rasterImagesIn(pdf), "The drawing stays vector, it is not rasterized to a picture");
        String text = textOf(pdf);
        assertTrue(text.contains("I am a test view"), "The text of the drawing is text in the PDF: " + text);
        assertTrue(text.contains("Some random text, on a page"), "The text of the drawing is text in the PDF: " + text);
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

    /** The names of the image XObjects the pages draw, directly or inside a form, as a vector SVG is drawn in one. */
    @SneakyThrows
    private static @NotNull List<String> rasterImagesIn(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> images = new ArrayList<>();
            Set<COSBase> seen = new HashSet<>();
            for (PDPage page : document.getPages()) {
                collectRasterImages(page.getResources(), images, seen);
            }
            return images;
        }
    }

    @SneakyThrows
    private static void collectRasterImages(PDResources resources, @NotNull List<String> images, @NotNull Set<COSBase> seen) {
        if (resources == null) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (!seen.add(xObject.getCOSObject())) {
                continue;
            }
            if (xObject instanceof PDImageXObject) {
                images.add(name.getName());
            } else if (xObject instanceof PDFormXObject form) {
                collectRasterImages(form.getResources(), images, seen);
            }
        }
    }

    @SneakyThrows
    private static @NotNull String textOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }
}
