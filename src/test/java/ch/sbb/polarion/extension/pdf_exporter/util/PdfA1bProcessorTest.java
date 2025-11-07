package ch.sbb.polarion.extension.pdf_exporter.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfA1bProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void testProcessPdfA1b_WithTransparentImages() throws IOException {
        // Create a PDF with images that have transparency masks
        byte[] pdfBytes = createPdfWithTransparencyMasks();

        // Process the PDF
        byte[] processedPdf = PdfA1bProcessor.processPdfA1b(pdfBytes);

        // Verify the result
        assertNotNull(processedPdf);
        assertTrue(processedPdf.length > 0);

        // Load the processed PDF and verify masks are removed
        try (PDDocument doc = Loader.loadPDF(processedPdf)) {
            PDPage page = doc.getPage(0);
            PDResources resources = page.getResources();
            assertNotNull(resources);

            // Check that images no longer have transparency masks
            for (COSName name : resources.getXObjectNames()) {
                PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                COSDictionary imageDict = image.getCOSObject();
                assertFalse(imageDict.containsKey(COSName.SMASK), "SMask should be removed");
                assertFalse(imageDict.containsKey(COSName.MASK), "Mask should be removed");
            }
        }
    }

    @Test
    void testProcessPdfA1b_WithoutTransparency() throws IOException {
        // Create a simple PDF without transparency
        byte[] pdfBytes = createSimplePdf();

        // Process the PDF
        byte[] processedPdf = PdfA1bProcessor.processPdfA1b(pdfBytes);

        // Verify the result
        assertNotNull(processedPdf);
        assertTrue(processedPdf.length > 0);
    }

    @Test
    void testProcessPdfA1b_EmptyDocument() throws IOException {
        // Create an empty PDF
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        // Process the PDF
        byte[] processedPdf = PdfA1bProcessor.processPdfA1b(pdfBytes);

        // Verify the result
        assertNotNull(processedPdf);
        assertTrue(processedPdf.length > 0);
    }

    @Test
    void testRemoveImageTransparency_WithMasks() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Create an image with both SMask and Mask
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");
            COSDictionary imageDict = image.getCOSObject();
            imageDict.setItem(COSName.SMASK, COSName.A);
            imageDict.setItem(COSName.MASK, COSName.A);

            // Add image to page resources
            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("Im1"), image);
            page.setResources(resources);

            // Process the document
            PdfA1bProcessor.removeImageTransparency(doc);

            // Verify masks are removed
            assertFalse(imageDict.containsKey(COSName.SMASK));
            assertFalse(imageDict.containsKey(COSName.MASK));
        }
    }

    @Test
    void testRemoveImageTransparency_WithoutMasks() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Create an image without masks
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");

            // Add image to page resources
            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("Im1"), image);
            page.setResources(resources);

            // Process the document (should not throw any exceptions)
            PdfA1bProcessor.removeImageTransparency(doc);

            // Verify no masks are present
            COSDictionary imageDict = image.getCOSObject();
            assertFalse(imageDict.containsKey(COSName.SMASK));
            assertFalse(imageDict.containsKey(COSName.MASK));
        }
    }

    @Test
    void testRemoveImageTransparency_NullResources() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            page.setResources(null);
            doc.addPage(page);

            // Process the document (should not throw any exceptions)
            PdfA1bProcessor.removeImageTransparency(doc);

            // No assertions needed - just verify no exception is thrown
        }
    }

    @Test
    void testRemoveTransparencyMasks_WithImages() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            // Create resources with two images
            PDResources resources = new PDResources();

            // Image 1: has SMask
            BufferedImage bufferedImage1 = createTestImage(50, 50);
            PDImageXObject image1 = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage1), "test1");
            image1.getCOSObject().setItem(COSName.SMASK, COSName.A);
            resources.put(COSName.getPDFName("Im1"), image1);

            // Image 2: has Mask
            BufferedImage bufferedImage2 = createTestImage(50, 50);
            PDImageXObject image2 = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage2), "test2");
            image2.getCOSObject().setItem(COSName.MASK, COSName.A);
            resources.put(COSName.getPDFName("Im2"), image2);

            // Process resources
            int masksRemoved = PdfA1bProcessor.removeTransparencyMasks(resources);

            // Verify both masks were removed
            assertEquals(2, masksRemoved);
            assertFalse(image1.getCOSObject().containsKey(COSName.SMASK));
            assertFalse(image2.getCOSObject().containsKey(COSName.MASK));
        }
    }

    @Test
    void testRemoveTransparencyMasks_WithoutImages() {
        PDResources resources = new PDResources();

        // Process empty resources
        int masksRemoved = PdfA1bProcessor.removeTransparencyMasks(resources);

        // Verify no masks were removed
        assertEquals(0, masksRemoved);
    }

    @Test
    void testRemoveMasksFromImage_WithSMask() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");
            COSDictionary imageDict = image.getCOSObject();
            imageDict.setItem(COSName.SMASK, COSName.A);

            // Remove masks
            int removed = PdfA1bProcessor.removeMasksFromImage(image);

            // Verify
            assertEquals(1, removed);
            assertFalse(imageDict.containsKey(COSName.SMASK));
        }
    }

    @Test
    void testRemoveMasksFromImage_WithMask() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");
            COSDictionary imageDict = image.getCOSObject();
            imageDict.setItem(COSName.MASK, COSName.A);

            // Remove masks
            int removed = PdfA1bProcessor.removeMasksFromImage(image);

            // Verify
            assertEquals(1, removed);
            assertFalse(imageDict.containsKey(COSName.MASK));
        }
    }

    @Test
    void testRemoveMasksFromImage_WithBothMasks() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");
            COSDictionary imageDict = image.getCOSObject();
            imageDict.setItem(COSName.SMASK, COSName.A);
            imageDict.setItem(COSName.MASK, COSName.A);

            // Remove masks
            int removed = PdfA1bProcessor.removeMasksFromImage(image);

            // Verify
            assertEquals(2, removed);
            assertFalse(imageDict.containsKey(COSName.SMASK));
            assertFalse(imageDict.containsKey(COSName.MASK));
        }
    }

    @Test
    void testRemoveMasksFromImage_WithoutMasks() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");

            // Remove masks
            int removed = PdfA1bProcessor.removeMasksFromImage(image);

            // Verify
            assertEquals(0, removed);
        }
    }

    @Test
    void testProcessPdfA1b_MultiplePages() throws IOException {
        // Create a PDF with multiple pages, each with masked images
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (int i = 0; i < 3; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                BufferedImage bufferedImage = createTestImage(100, 100);
                PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test" + i);
                image.getCOSObject().setItem(COSName.SMASK, COSName.A);

                PDResources resources = new PDResources();
                resources.put(COSName.getPDFName("Im" + i), image);
                page.setResources(resources);
            }

            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        // Process the PDF
        byte[] processedPdf = PdfA1bProcessor.processPdfA1b(pdfBytes);

        // Verify all masks are removed from all pages
        try (PDDocument doc = Loader.loadPDF(processedPdf)) {
            assertEquals(3, doc.getNumberOfPages());

            for (PDPage page : doc.getPages()) {
                PDResources resources = page.getResources();
                for (COSName name : resources.getXObjectNames()) {
                    PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                    assertFalse(image.getCOSObject().containsKey(COSName.SMASK));
                    assertFalse(image.getCOSObject().containsKey(COSName.MASK));
                }
            }
        }
    }

    // Helper methods

    private byte[] createPdfWithTransparencyMasks() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Create an image with SMask
            BufferedImage bufferedImage = createTestImage(100, 100);
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, toByteArray(bufferedImage), "test");
            COSDictionary imageDict = image.getCOSObject();
            imageDict.setItem(COSName.SMASK, COSName.A);

            // Add image to page
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.drawImage(image, 100, 100, 100, 100);
            contentStream.close();

            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createSimplePdf() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private byte[] toByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
