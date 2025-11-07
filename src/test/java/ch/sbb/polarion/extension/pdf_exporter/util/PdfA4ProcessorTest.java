package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfA4ProcessorTest {

    @Test
    @SneakyThrows
    void testProcessPdfA4_withValidPdf() {
        // Create a minimal valid PDF with PDF/A-4 metadata
        byte[] pdfBytes = createMinimalPdfWithMetadata(createPdfA4Metadata(false, false));

        // Process it
        byte[] result = PdfA4Processor.processPdfA4(pdfBytes);

        // Verify it's still a valid PDF
        assertThat(result)
                .isNotNull()
                .hasSizeGreaterThan(0);

        // Verify metadata was fixed
        try (PDDocument doc = Loader.loadPDF(result)) {
            PDMetadata metadata = doc.getDocumentCatalog().getMetadata();
            assertThat(metadata).isNotNull();

            String metadataStr = new String(metadata.toByteArray(), StandardCharsets.UTF_8);
            assertThat(metadataStr)
                    .contains("pdfaid:rev=\"2020\"")
                    .doesNotContain("pdfaid:conformance");
        }
    }

    @Test
    @SneakyThrows
    void testFixDocumentInformation_noPieceInfo() {
        // Create PDF without PieceInfo
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle("Test Title");
            info.setAuthor("Test Author");
            info.setModificationDate(Calendar.getInstance());
            doc.setDocumentInformation(info);

            // Process
            PdfA4Processor.fixDocumentInformation(doc);

            // Verify Info was removed from trailer
            COSDictionary trailer = doc.getDocument().getTrailer();
            assertThat(trailer.containsKey(COSName.INFO)).isFalse();
        }
    }

    @Test
    @SneakyThrows
    void testFixDocumentInformation_withPieceInfo() {
        // Create PDF with PieceInfo
        try (PDDocument doc = new PDDocument()) {
            // Add PieceInfo to catalog
            COSDictionary catalog = doc.getDocumentCatalog().getCOSObject();
            catalog.setItem(COSName.PIECE_INFO, new COSDictionary());

            // Add document information with multiple fields
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle("Test Title");
            info.setAuthor("Test Author");
            Calendar modDate = Calendar.getInstance();
            info.setModificationDate(modDate);
            doc.setDocumentInformation(info);

            // Process
            PdfA4Processor.fixDocumentInformation(doc);

            // Verify only ModDate remains
            PDDocumentInformation resultInfo = doc.getDocumentInformation();
            assertThat(resultInfo).isNotNull();
            assertThat(resultInfo.getModificationDate()).isNotNull();
            assertThat(resultInfo.getTitle()).isNull();
            assertThat(resultInfo.getAuthor()).isNull();
        }
    }

    @Test
    @SneakyThrows
    void testFixDocumentInformation_withPieceInfoNoModDate() {
        // Create PDF with PieceInfo but no ModDate
        try (PDDocument doc = new PDDocument()) {
            // Add PieceInfo to catalog
            COSDictionary catalog = doc.getDocumentCatalog().getCOSObject();
            catalog.setItem(COSName.PIECE_INFO, new COSDictionary());

            // Add document information without ModDate
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle("Test Title");
            doc.setDocumentInformation(info);

            // Process
            PdfA4Processor.fixDocumentInformation(doc);

            // Verify ModDate was added
            PDDocumentInformation resultInfo = doc.getDocumentInformation();
            assertThat(resultInfo).isNotNull();
            assertThat(resultInfo.getModificationDate()).isNotNull();
        }
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadata_noMetadata() {
        // Create PDF without metadata
        try (PDDocument doc = new PDDocument()) {
            // Process - should not throw exception
            PdfA4Processor.fixXmpMetadata(doc);

            // Verify still no metadata
            assertThat(doc.getDocumentCatalog().getMetadata()).isNull();
        }
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadata_withPdfA4Metadata() {
        // Create PDF with PDF/A-4 metadata (missing rev)
        try (PDDocument doc = new PDDocument()) {
            String xmpMetadata = createPdfA4Metadata(false, false);
            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(xmpMetadata.getBytes(StandardCharsets.UTF_8));
            doc.getDocumentCatalog().setMetadata(metadata);

            // Process
            PdfA4Processor.fixXmpMetadata(doc);

            // Verify metadata was fixed
            String result = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertThat(result).contains("pdfaid:rev=\"2020\"");
        }
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadata_withInvalidXml() {
        // Create PDF with invalid XML metadata
        try (PDDocument doc = new PDDocument()) {
            String invalidXml = """
                    <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
                    <invalid>broken xml<</invalid>
                    <?xpacket end="r"?>
                    """;
            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(invalidXml.getBytes(StandardCharsets.UTF_8));
            doc.getDocumentCatalog().setMetadata(metadata);

            // Process - should throw IOException
            assertThatThrownBy(() -> PdfA4Processor.fixXmpMetadata(doc))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Failed to process XMP metadata");
        }
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadataXml_addMissingRev() {
        String xmpMetadata = createPdfA4Metadata(false, false);

        String result = PdfA4Processor.fixXmpMetadataXml(xmpMetadata);

        assertThat(result).contains("pdfaid:rev=\"2020\"");
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadataXml_updateIncorrectRev() {
        String xmpMetadata = createPdfA4Metadata(true, false);

        String result = PdfA4Processor.fixXmpMetadataXml(xmpMetadata);

        assertThat(result)
                .contains("pdfaid:rev=\"2020\"")
                .doesNotContain("pdfaid:rev=\"4\"");
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadataXml_removeConformance() {
        String xmpMetadata = createPdfA4Metadata(false, true);

        String result = PdfA4Processor.fixXmpMetadataXml(xmpMetadata);

        assertThat(result)
                .doesNotContain("pdfaid:conformance")
                .contains("pdfaid:rev=\"2020\"");
    }

    @Test
    @SneakyThrows
    void testFixXmpMetadataXml_nonPdfA4() {
        String xmpMetadata = """
                <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about="" />
                </rdf:RDF>
                <?xpacket end="r"?>
                """;

        String result = PdfA4Processor.fixXmpMetadataXml(xmpMetadata);

        // Should return unchanged
        assertThat(result).isEqualTo(xmpMetadata);
    }

    @Test
    @SneakyThrows
    void testExtractRdfContent() {
        String xmpMetadata = """
                <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">content</rdf:RDF>
                <?xpacket end="r"?>
                """;

        String result = PdfA4Processor.extractRdfContent(xmpMetadata);

        assertThat(result).isEqualTo("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">content</rdf:RDF>");
    }

    @Test
    @SneakyThrows
    void testExtractRdfContent_noXpacket() {
        String xmpMetadata = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">content</rdf:RDF>";

        String result = PdfA4Processor.extractRdfContent(xmpMetadata);

        assertThat(result).isEqualTo("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">content</rdf:RDF>");
    }

    @Test
    @SneakyThrows
    void testExtractRdfContent_onlyStartXpacket() {
        String xmpMetadata = "<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">content</rdf:RDF>";

        String result = PdfA4Processor.extractRdfContent(xmpMetadata);

        assertThat(result).isEqualTo("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">content</rdf:RDF>");
    }

    @Test
    void testWrapWithXPacket() {
        String rdfContent = "<rdf:RDF>content</rdf:RDF>";

        String result = PdfA4Processor.wrapWithXPacket(rdfContent);

        assertThat(result)
                .startsWith("<?xpacket begin=\"\"")
                .contains(rdfContent)
                .endsWith("<?xpacket end=\"r\"?>");
    }

    @Test
    void testWrapWithXPacket_generatesUniqueIds() {
        String rdfContent = "<rdf:RDF>content</rdf:RDF>";

        // Generate multiple XMP packets
        String result1 = PdfA4Processor.wrapWithXPacket(rdfContent);
        String result2 = PdfA4Processor.wrapWithXPacket(rdfContent);

        // Extract IDs from both results
        String id1 = result1.substring(result1.indexOf("id=\"") + 4, result1.indexOf("\"?>"));
        String id2 = result2.substring(result2.indexOf("id=\"") + 4, result2.indexOf("\"?>"));

        // Verify IDs are different (unique)
        // Verify ID format (UUID without hyphens = 32 hex chars)
        assertThat(id1)
                .isNotEqualTo(id2)
                .hasSize(32)
                .matches("[a-f0-9]{32}");
        assertThat(id2)
                .hasSize(32)
                .matches("[a-f0-9]{32}");
    }

    @Test
    @SneakyThrows
    void testFixPdfA4Identification_addRev() {
        Element element = createPdfA4DescriptionElement(false, false);

        boolean modified = PdfA4Processor.fixPdfA4Identification(element);

        assertThat(modified).isTrue();
        assertThat(element.getAttribute("pdfaid:rev")).isEqualTo("2020");
    }

    @Test
    @SneakyThrows
    void testFixPdfA4Identification_updateRev() {
        Element element = createPdfA4DescriptionElement(true, false);

        boolean modified = PdfA4Processor.fixPdfA4Identification(element);

        assertThat(modified).isTrue();
        assertThat(element.getAttribute("pdfaid:rev")).isEqualTo("2020");
    }

    @Test
    @SneakyThrows
    void testFixPdfA4Identification_removeConformance() {
        Element element = createPdfA4DescriptionElement(false, true);

        boolean modified = PdfA4Processor.fixPdfA4Identification(element);

        assertThat(modified).isTrue();
        assertThat(element.hasAttribute("pdfaid:conformance")).isFalse();
    }

    @Test
    @SneakyThrows
    void testFixPdfA4Identification_alreadyCorrect() {
        Element element = createPdfA4DescriptionElement(false, false);
        element.setAttribute("pdfaid:rev", "2020");

        boolean modified = PdfA4Processor.fixPdfA4Identification(element);

        assertThat(modified).isFalse();
    }

    @Test
    @SneakyThrows
    void testGetAttributeValue_withAttribute() {
        Element element = createPdfA4DescriptionElement(false, false);

        String value = PdfA4Processor.getAttributeValue(element, "pdfaid:part");

        assertThat(value).isEqualTo("4");
    }

    @Test
    @SneakyThrows
    void testGetAttributeValue_withoutAttribute() {
        Element element = createPdfA4DescriptionElement(false, false);

        String value = PdfA4Processor.getAttributeValue(element, "pdfaid:nonexistent");

        assertThat(value).isNull();
    }

    @Test
    void testGetNamespaceUriForPrefix_pdfaid() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("pdfaid");
        assertThat(uri).isEqualTo("http://www.aiim.org/pdfa/ns/id/");
    }

    @Test
    void testGetNamespaceUriForPrefix_pdf() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("pdf");
        assertThat(uri).isEqualTo("http://ns.adobe.com/pdf/1.3/");
    }

    @Test
    void testGetNamespaceUriForPrefix_rdf() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("rdf");
        assertThat(uri).isEqualTo("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }

    @Test
    void testGetNamespaceUriForPrefix_dc() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("dc");
        assertThat(uri).isEqualTo("http://purl.org/dc/elements/1.1/");
    }

    @Test
    void testGetNamespaceUriForPrefix_xmp() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("xmp");
        assertThat(uri).isEqualTo("http://ns.adobe.com/xap/1.0/");
    }

    @Test
    void testGetNamespaceUriForPrefix_unknown() {
        String uri = PdfA4Processor.getNamespaceUriForPrefix("unknown");
        assertThat(uri).isNull();
    }

    @Test
    @SneakyThrows
    void testDocumentToString() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader("<root><child>text</child></root>")));

        String result = PdfA4Processor.documentToString(doc);

        assertThat(result)
                .contains("<root>")
                .contains("<child>text</child>")
                .contains("</root>")
                .doesNotContain("<?xml");
    }

    // Helper methods

    @SneakyThrows
    private Element createPdfA4DescriptionElement(boolean withIncorrectRev, boolean withConformance) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element element = doc.createElement("rdf:Description");
        element.setAttribute("pdfaid:part", "4");

        if (withIncorrectRev) {
            element.setAttribute("pdfaid:rev", "4");
        }

        if (withConformance) {
            element.setAttribute("pdfaid:conformance", "B");
        }

        return element;
    }

    private String createPdfA4Metadata(boolean withIncorrectRev, boolean withConformance) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n");
        sb.append("<rdf:RDF xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\" ");
        sb.append("xmlns:pdfaid=\"http://www.aiim.org/pdfa/ns/id/\" ");
        sb.append("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
        sb.append("<rdf:Description rdf:about=\"\" pdfaid:part=\"4\"");

        if (withIncorrectRev) {
            sb.append(" pdfaid:rev=\"4\"");
        }

        if (withConformance) {
            sb.append(" pdfaid:conformance=\"B\"");
        }

        sb.append(" />");
        sb.append("<rdf:Description rdf:about=\"\" pdf:Producer=\"WeasyPrint 66.0\" />");
        sb.append("</rdf:RDF>\n");
        sb.append("<?xpacket end=\"r\"?>");
        return sb.toString();
    }

    @SneakyThrows
    private byte[] createMinimalPdfWithMetadata(String xmpMetadata) {
        try (PDDocument doc = new PDDocument()) {
            // Add a blank page
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());

            // Add metadata
            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(xmpMetadata.getBytes(StandardCharsets.UTF_8));
            doc.getDocumentCatalog().setMetadata(metadata);

            // Save to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
