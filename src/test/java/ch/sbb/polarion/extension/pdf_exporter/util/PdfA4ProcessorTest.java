package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class PdfA4ProcessorTest extends BaseWeasyPrintTest {

    @Test
    @SneakyThrows
    void testPdfA4Processing() {
        // Generate a simple PDF/A-4b document
        String html = "<html><body><h1>Test PDF/A-4</h1><p>This is a test document.</p></body></html>";

        WeasyPrintOptions options = WeasyPrintOptions.builder()
                .pdfVariant(PdfVariant.PDF_A_4B)
                .build();

        byte[] originalPdf = exportToPdf(html, options);

        // Extract and print original metadata for debugging
        String originalMetadata = extractMetadata(originalPdf);
        System.out.println("=== Original metadata ===");
        System.out.println(originalMetadata);
        System.out.println("=== End original metadata ===");

        // Process the PDF
        byte[] processedPdf = PdfA4Processor.processPdfA4(originalPdf);

        // Extract and print processed metadata
        String processedMetadata = extractMetadata(processedPdf);
        System.out.println("\n=== Processed metadata ===");
        System.out.println(processedMetadata);
        System.out.println("=== End processed metadata ===");

        // Verify that metadata was changed
        assertNotNull(processedPdf);
        assertTrue(processedPdf.length > 0);

        // Write files for debugging
        java.nio.file.Files.write(
                java.nio.file.Paths.get("/tmp/test-pdfa4-original.pdf"),
                originalPdf
        );
        java.nio.file.Files.write(
                java.nio.file.Paths.get("/tmp/test-pdfa4-processed.pdf"),
                processedPdf
        );

        System.out.println("\nPDF files written to:");
        System.out.println("  /tmp/test-pdfa4-original.pdf");
        System.out.println("  /tmp/test-pdfa4-processed.pdf");

        // Verify specific fixes
        assertTrue(processedMetadata.contains("2020"),
            "Metadata should contain year 2020. Actual metadata:\n" + processedMetadata);
        assertFalse(processedMetadata.contains("<pdfaid:conformance>"),
            "Metadata should not contain conformance element. Actual metadata:\n" + processedMetadata);
    }

    @SneakyThrows
    private String extractMetadata(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDMetadata metadata = document.getDocumentCatalog().getMetadata();
            if (metadata != null) {
                return new String(metadata.toByteArray(), StandardCharsets.UTF_8);
            }
            return null;
        }
    }
}
