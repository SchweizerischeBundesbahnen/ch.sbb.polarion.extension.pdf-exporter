package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfMetadataTest extends BaseWeasyPrintTest {

    @Test
    @SneakyThrows
    void testMetadata() {

        String testName = getCurrentMethodName();

        byte[] documentBytes = exportToPdf("""
                <html>
                <head>
                    <meta name="field1" content="value1">
                    <meta name="field2" content="value2">
                </head>
                <body>
                    <p>Some document</p>
                </body>
                </html>
                """, WeasyPrintOptions.builder().customMetadata(true).build());

        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDDocumentInformation info = document.getDocumentInformation();
            Map<String, String> convertedMetadata = new HashMap<>();
            for (String key : info.getMetadataKeys()) {
                convertedMetadata.put(key, info.getCustomMetadataValue(key));
            }
            assertTrue(convertedMetadata.containsKey("field1"));
            assertTrue(convertedMetadata.containsKey("field2"));
            assertEquals("value1", convertedMetadata.get("field1"));
            assertEquals("value2", convertedMetadata.get("field2"));
        }

        writeReportPdf(testName, "content", documentBytes);
    }
}
