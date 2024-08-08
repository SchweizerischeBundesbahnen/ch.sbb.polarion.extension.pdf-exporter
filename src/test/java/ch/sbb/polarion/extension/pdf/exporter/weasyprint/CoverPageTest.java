package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverPageTest extends BaseWeasyPrintTest {

    @Test
    @SneakyThrows
    void testCorruptedContentOnMerge() {

        String testName = getCurrentMethodName();

        byte[] titleBytes = exportToPdf("<div>HEADER</div>", new WeasyPrintOptions(true));
        byte[] contentBytes = exportToPdf("<div style='break-after:page'>page to be removed</div><div>TEST</div>", new WeasyPrintOptions(true));

        BufferedImage titleImage;
        BufferedImage contentImage;
        try (PDDocument titleDoc = Loader.loadPDF(titleBytes);
             PDDocument contentDoc = Loader.loadPDF(contentBytes)) {
            titleImage = MediaUtils.pdfPageToImage(titleDoc, 0);
            contentImage = MediaUtils.pdfPageToImage(contentDoc, 1);
        }

        writeReportPdf(testName, "title", titleBytes);
        writeReportPdf(testName, "content", contentBytes);

        byte[] resultBytes = MediaUtils.overwriteFirstPageWithTitle(contentBytes, titleBytes);
        writeReportPdf(testName, "correct", resultBytes);
        try (PDDocument resultDoc = Loader.loadPDF(resultBytes)) {
            assertTrue(MediaUtils.compareImages(titleImage, MediaUtils.pdfPageToImage(resultDoc, 0))); //now the title remains the same
            assertTrue(MediaUtils.compareImages(contentImage, MediaUtils.pdfPageToImage(resultDoc, 1))); //initial content too
        }
    }

    @Test
    @SneakyThrows
    void testOnlyFirstTitlePageTakenOnMerge() {

        String testName = getCurrentMethodName();

        byte[] titleBytes = exportToPdf("<div style='break-after:page'>HEADER 1</div><div style='break-after:page'>HEADER 2</div><div style='break-after:page'>HEADER 3</div>", new WeasyPrintOptions(true));
        byte[] contentBytes = exportToPdf("<div style='break-after:page'>page to be removed</div><div>TEST</div>", new WeasyPrintOptions(true));

        BufferedImage titleImage;
        BufferedImage contentImage;
        try (PDDocument titleDoc = Loader.loadPDF(titleBytes);
             PDDocument contentDoc = Loader.loadPDF(contentBytes)) {
            assertEquals(3, titleDoc.getNumberOfPages()); //title pdf has 3 pages
            titleImage = MediaUtils.pdfPageToImage(titleDoc, 0);
            assertEquals(2, contentDoc.getNumberOfPages());
            contentImage = MediaUtils.pdfPageToImage(contentDoc, 1);
        }

        writeReportPdf(testName, "title", titleBytes);
        writeReportPdf(testName, "content", contentBytes);

        byte[] resultBytes = MediaUtils.overwriteFirstPageWithTitle(contentBytes, titleBytes);
        writeReportPdf(testName, "merged", resultBytes);
        try (PDDocument resultDoc = Loader.loadPDF(resultBytes)) {
            assertEquals(2, resultDoc.getNumberOfPages());
            assertTrue(MediaUtils.compareImages(titleImage, MediaUtils.pdfPageToImage(resultDoc, 0))); //page #1 is the first one taken from title
            assertTrue(MediaUtils.compareImages(contentImage, MediaUtils.pdfPageToImage(resultDoc, 1))); //the second is the content itself
        }
    }
}
