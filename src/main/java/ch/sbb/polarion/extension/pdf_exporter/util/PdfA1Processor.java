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
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Utility class for processing PDF/A-1 documents to ensure compliance with ISO 19005-1:2005 specification.
 * <p>
 * This processor handles the following compliance issues:
 * <ul>
 *     <li>PDF/A-1 does not support transparency - removes transparency masks (SMask and Mask) from all images</li>
 *     <li>PDF/A-1a requires all non-standard structure types to be mapped to standard types in RoleMap
 *         (ISO 19005-1:2005, clause 6.8.3.4) - adds mappings for HTML5 table structure elements
 *         (TBody, THead, TFoot) that WeasyPrint generates but are not in PDF 1.4 standard structure types</li>
 * </ul>
 */
@UtilityClass
public class PdfA1Processor {
    private static final Logger logger = Logger.getLogger(PdfA1Processor.class);

    /**
     * Standard PDF 1.4 structure type for table elements.
     */
    private static final String STRUCTURE_TYPE_TABLE = "Table";

    /**
     * Non-standard structure types used by WeasyPrint for HTML5 table elements
     * mapped to their nearest standard PDF 1.4 equivalents.
     * <p>
     * According to PDF Reference 9.7.4, standard table structure types are:
     * Table, TR, TH, TD, THead, TBody, TFoot (but THead/TBody/TFoot were added in PDF 1.5).
     * For PDF/A-1 (based on PDF 1.4), we map these to Table as the nearest equivalent.
     */
    private static final Map<String, String> NON_STANDARD_ROLE_MAPPINGS = Map.of(
            "TBody", STRUCTURE_TYPE_TABLE,
            "THead", STRUCTURE_TYPE_TABLE,
            "TFoot", STRUCTURE_TYPE_TABLE
    );

    /**
     * Processes a PDF/A-1 document to fix compliance issues according to ISO 19005-1:2005.
     * <p>
     * This method applies the following fixes:
     * <ul>
     *     <li>Removes transparency masks from images (PDF/A-1 does not support transparency)</li>
     *     <li>Adds RoleMap entries for non-standard structure types (required for PDF/A-1a)</li>
     * </ul>
     *
     * @param pdfBytes the original PDF content
     * @return the processed PDF content with compliance fixes applied
     * @throws IOException if an error occurs during PDF processing
     */
    public byte[] processPdfA1(byte[] pdfBytes) throws IOException {
        return processPdfA1(pdfBytes, null);
    }

    /**
     * Processes a PDF/A-1 document to fix compliance issues according to ISO 19005-1:2005.
     * <p>
     * This method applies the following fixes:
     * <ul>
     *     <li>Removes transparency masks from images (PDF/A-1 does not support transparency)</li>
     *     <li>Adds RoleMap entries for non-standard structure types (required for PDF/A-1a)</li>
     * </ul>
     *
     * @param pdfBytes    the original PDF content
     * @param conformance the conformance level: "A" for accessible (tagged), "B" for basic, or null (defaults to "B")
     * @return the processed PDF content with compliance fixes applied
     * @throws IOException if an error occurs during PDF processing
     */
    public byte[] processPdfA1(byte[] pdfBytes, @Nullable String conformance) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Log conformance level if specified
            if (conformance != null) {
                logger.debug("Processing PDF/A-1" + conformance.toLowerCase() + " document");
            }

            // Process all pages to remove transparency from images
            removeImageTransparency(document);

            // Fix RoleMap for non-standard structure types (required only for PDF/A-1a)
            // PDF/A-1b does not require tagged structure, so RoleMap fix is not needed
            if ("A".equalsIgnoreCase(conformance)) {
                fixStructureTreeRoleMap(document);
            }

            // Save the modified document without using xref streams (PDF/A-1 requirement)
            // PDF/A-1 does not allow xref streams (ISO 19005-1:2005, clause 6.1.4, test 3)
            CompressParameters compressParameters = CompressParameters.NO_COMPRESSION;
            document.save(outputStream, compressParameters);
            return outputStream.toByteArray();
        }
    }

    /**
     * Fixes the RoleMap in the document's StructureTreeRoot to map non-standard structure types
     * to their nearest standard equivalents as required by ISO 19005-1:2005, clause 6.8.3.4.
     * <p>
     * WeasyPrint generates HTML5 table structure elements (TBody, THead, TFoot) which are not
     * standard structure types in PDF 1.4. This method adds mappings for these elements.
     * <p>
     * Note: This fix is required for PDF/A-1a compliance. For PDF/A-1b (which doesn't require
     * tagged structure), this method will have no effect if there's no StructureTreeRoot.
     *
     * @param document the PDF document to process
     */
    @VisibleForTesting
    void fixStructureTreeRoleMap(@NotNull PDDocument document) {
        PDStructureTreeRoot structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();
        if (structureTreeRoot == null) {
            logger.debug("No StructureTreeRoot found, skipping RoleMap fix");
            return;
        }

        COSDictionary structTreeRootDict = structureTreeRoot.getCOSObject();

        // Get or create RoleMap dictionary
        COSDictionary roleMap = (COSDictionary) structTreeRootDict.getDictionaryObject(COSName.ROLE_MAP);
        if (roleMap == null) {
            roleMap = new COSDictionary();
            structTreeRootDict.setItem(COSName.ROLE_MAP, roleMap);
            logger.debug("Created new RoleMap dictionary in StructureTreeRoot");
        }

        // Add mappings for non-standard structure types
        int mappingsAdded = 0;
        for (Map.Entry<String, String> entry : NON_STANDARD_ROLE_MAPPINGS.entrySet()) {
            COSName nonStandardType = COSName.getPDFName(entry.getKey());
            COSName standardType = COSName.getPDFName(entry.getValue());

            if (!roleMap.containsKey(nonStandardType)) {
                roleMap.setItem(nonStandardType, standardType);
                mappingsAdded++;
                logger.debug("Added RoleMap entry: " + entry.getKey() + " -> " + entry.getValue());
            }
        }

        if (mappingsAdded > 0) {
            logger.info("Added " + mappingsAdded + " RoleMap entries for PDF/A-1a compliance");
        } else {
            logger.debug("All required RoleMap entries already present");
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
            logger.info("Removed " + masksRemoved + " transparency mask(s) for PDF/A-1 compliance");
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
