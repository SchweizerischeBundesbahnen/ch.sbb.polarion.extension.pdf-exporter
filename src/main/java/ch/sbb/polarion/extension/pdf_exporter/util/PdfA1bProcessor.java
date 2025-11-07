package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.core.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility class for processing PDF/A-1b documents to ensure compliance with ISO 19005-1:2005 specification.
 * <p>
 * PDF/A-1b does not support transparency. This processor removes transparency masks (SMask and Mask)
 * from all images in the document.
 */
@UtilityClass
public class PdfA1bProcessor {
    private static final Logger logger = Logger.getLogger(PdfA1bProcessor.class);

    /**
     * Processes a PDF/A-1b document to fix compliance issues according to ISO 19005-1:2005.
     *
     * @param pdfBytes the original PDF content
     * @return the processed PDF content with compliance fixes applied
     * @throws IOException if an error occurs during PDF processing
     */
    public byte[] processPdfA1b(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Process all pages to remove transparency from images
            removeImageTransparency(document);

            // Save the modified document without using xref streams (PDF/A-1b requirement)
            // PDF/A-1b does not allow xref streams (ISO 19005-1:2005, clause 6.1.4, test 3)
            CompressParameters compressParameters = CompressParameters.NO_COMPRESSION;
            document.save(outputStream, compressParameters);
            return outputStream.toByteArray();
        }
    }

    /**
     * Removes transparency from all images in the document.
     * Simply removes SMask and Mask entries from image dictionaries.
     *
     * @param document the PDF document to process
     */
    @VisibleForTesting
    void removeImageTransparency(@NotNull PDDocument document) {
        int masksRemoved = 0;

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources != null) {
                masksRemoved += removeTransparencyMasks(resources);
            }
        }

        if (masksRemoved > 0) {
            logger.info("Removed " + masksRemoved + " transparency mask(s) for PDF/A-1b compliance");
        } else {
            logger.debug("No transparency masks found in document");
        }
    }

    /**
     * Removes transparency masks from all images in resources.
     *
     * @param resources the PDF resources to process
     * @return the number of masks removed
     */
    @VisibleForTesting
    int removeTransparencyMasks(@NotNull PDResources resources) {
        int masksRemoved = 0;

        for (COSName name : resources.getXObjectNames()) {
            try {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject imageXObject) {
                    masksRemoved += removeMasksFromImage(imageXObject);
                }
            } catch (IOException e) {
                logger.warn("Failed to process XObject: " + name.getName(), e);
            }
        }

        return masksRemoved;
    }

    /**
     * Removes transparency masks (SMask and Mask) from an image.
     *
     * @param image the image to process
     * @return the number of masks removed (0, 1, or 2)
     */
    @VisibleForTesting
    int removeMasksFromImage(@NotNull PDImageXObject image) {
        COSDictionary imageDict = image.getCOSObject();
        int removed = 0;

        if (imageDict.containsKey(COSName.SMASK)) {
            imageDict.removeItem(COSName.SMASK);
            removed++;
            logger.debug("Removed SMask from image");
        }

        if (imageDict.containsKey(COSName.MASK)) {
            imageDict.removeItem(COSName.MASK);
            removed++;
            logger.debug("Removed Mask from image");
        }

        return removed;
    }
}
