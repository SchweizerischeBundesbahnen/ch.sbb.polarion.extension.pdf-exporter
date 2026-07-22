package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Applies variant-specific post-processing to a generated PDF to ensure ISO compliance
 * (PDF/A-1, PDF/A-4, PDF/UA-2). Shared by the single-document and bulk (merge) conversion paths.
 */
public class PdfPostProcessor {
    private static final Logger logger = Logger.getLogger(PdfPostProcessor.class);

    /**
     * Applies the compliance post-processing matching the given variant. Returns the input bytes
     * unchanged if {@code pdfVariant} is {@code null}, requires no post-processing, or if the
     * post-processing step fails (best-effort — the original PDF is preferred over a hard failure).
     */
    public byte[] postProcess(byte @NotNull [] pdfBytes, @Nullable PdfVariant pdfVariant, @Nullable PdfGenerationLog generationLog) {
        if (pdfVariant == null) {
            return pdfBytes;
        }

        // Post-process PDF/A-1 documents to ensure compliance with ISO 19005-1:2005
        if (isPdfA1Variant(pdfVariant)) {
            long startTime = System.currentTimeMillis();
            int originalSize = pdfBytes.length;
            pdfBytes = postProcessPdfA1(pdfBytes, pdfVariant);
            recordTiming(generationLog, "PDF/A-1 post-processing", System.currentTimeMillis() - startTime,
                    String.format("variant=%s, pdf_size=%d->%d bytes", pdfVariant, originalSize, pdfBytes.length));
        }

        // Post-process PDF/A-4 documents to ensure compliance with ISO 19005-4:2020
        if (isPdfA4Variant(pdfVariant)) {
            long startTime = System.currentTimeMillis();
            int originalSize = pdfBytes.length;
            pdfBytes = postProcessPdfA4(pdfBytes, pdfVariant);
            recordTiming(generationLog, "PDF/A-4 post-processing", System.currentTimeMillis() - startTime,
                    String.format("variant=%s, pdf_size=%d->%d bytes", pdfVariant, originalSize, pdfBytes.length));
        }

        // Post-process PDF/UA-2 documents to ensure compliance with ISO 14289-2:2024
        if (pdfVariant == PdfVariant.PDF_UA_2) {
            long startTime = System.currentTimeMillis();
            int originalSize = pdfBytes.length;
            pdfBytes = postProcessPdfUa2(pdfBytes);
            recordTiming(generationLog, "PDF/UA-2 post-processing", System.currentTimeMillis() - startTime,
                    String.format("pdf_size=%d->%d bytes", originalSize, pdfBytes.length));
        }

        return pdfBytes;
    }

    private void recordTiming(@Nullable PdfGenerationLog generationLog, String stageName, long durationMs, String details) {
        if (generationLog != null) {
            generationLog.recordTiming(stageName, durationMs, details);
        }
    }

    private boolean isPdfA1Variant(@NotNull PdfVariant pdfVariant) {
        return pdfVariant == PdfVariant.PDF_A_1A || pdfVariant == PdfVariant.PDF_A_1B;
    }

    private byte[] postProcessPdfA1(byte[] pdfBytes, @NotNull PdfVariant pdfVariant) {
        try {
            String conformance = switch (pdfVariant) {
                case PDF_A_1A -> "A";
                case PDF_A_1B -> "B";
                default -> null;
            };
            return PdfA1Processor.processPdfA1(pdfBytes, conformance);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/A-1 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
        }
    }

    private boolean isPdfA4Variant(@NotNull PdfVariant pdfVariant) {
        return pdfVariant == PdfVariant.PDF_A_4E || pdfVariant == PdfVariant.PDF_A_4F || pdfVariant == PdfVariant.PDF_A_4U;
    }

    private byte[] postProcessPdfA4(byte[] pdfBytes, @NotNull PdfVariant pdfVariant) {
        try {
            String conformance = switch (pdfVariant) {
                case PDF_A_4E -> "E";
                case PDF_A_4F -> "F";
                default -> null;
            };
            return PdfA4Processor.processPdfA4(pdfBytes, conformance);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/A-4 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
        }
    }

    private byte[] postProcessPdfUa2(byte[] pdfBytes) {
        try {
            return PdfUa2Processor.processPdfUa2(pdfBytes);
        } catch (IOException e) {
            logger.error("Failed to post-process PDF/UA-2 document for compliance", e);
            // Return original PDF if post-processing fails
            return pdfBytes;
        }
    }
}
