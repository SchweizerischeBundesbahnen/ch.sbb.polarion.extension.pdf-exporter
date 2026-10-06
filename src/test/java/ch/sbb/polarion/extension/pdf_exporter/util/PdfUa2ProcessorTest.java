package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The structure PDF/UA-2 requires of a merged document: PDFBox wraps the Document elements of the merged files, which
 * keep the PDF 2.0 namespace, in a Document of its own without one, and writes the document as PDF 1.6.
 */
class PdfUa2ProcessorTest {

    private static final COSName NS = COSName.getPDFName("NS");
    private static final COSName NAMESPACES = COSName.getPDFName("Namespaces");
    private static final String PDF_2_NAMESPACE = "http://iso.org/pdf2/ssn";

    @Test
    @SneakyThrows
    void givesTheDocumentOfAMergeTheNamespaceOfItsParts() {
        try (PDDocument document = new PDDocument()) {
            COSDictionary namespace = namespace();
            PDStructureElement merged = documentWithParts(document, namespace);

            PdfUa2Processor.fixStructureNamespace(document);

            assertThat(merged.getCOSObject().getItem(NS)).as("The Document names the namespace of its parts").isSameAs(namespace);
            COSArray namespaces = document.getDocumentCatalog().getStructureTreeRoot().getCOSObject().getCOSArray(NAMESPACES);
            assertThat(namespaces.size()).as("The root lists the namespace once").isEqualTo(1);
            assertThat(namespaces.get(0)).isSameAs(namespace);
        }
    }

    @Test
    @SneakyThrows
    void givesTheDocumentANewNamespaceWhereNoElementHasOne() {
        try (PDDocument document = new PDDocument()) {
            PDStructureElement merged = documentWithParts(document, null);

            PdfUa2Processor.fixStructureNamespace(document);

            COSBase namespace = merged.getCOSObject().getItem(NS);
            assertThat(namespace).isInstanceOf(COSDictionary.class);
            assertThat(((COSDictionary) namespace).getString(NS)).isEqualTo(PDF_2_NAMESPACE);
            COSArray namespaces = document.getDocumentCatalog().getStructureTreeRoot().getCOSObject().getCOSArray(NAMESPACES);
            assertThat(namespaces.size()).isEqualTo(1);
            assertThat(namespaces.get(0)).isSameAs(namespace);
        }
    }

    @Test
    @SneakyThrows
    void leavesADocumentWithItsNamespace() {
        try (PDDocument document = new PDDocument()) {
            COSDictionary namespace = namespace();
            PDStructureElement single = documentWithParts(document, namespace);
            single.getCOSObject().setItem(NS, namespace);

            PdfUa2Processor.fixStructureNamespace(document);

            assertThat(document.getDocumentCatalog().getStructureTreeRoot().getCOSObject().containsKey(NAMESPACES)).as("Nothing is touched").isFalse();
        }
    }

    @Test
    @SneakyThrows
    void leavesADocumentWithoutStructure() {
        try (PDDocument document = new PDDocument()) {
            PdfUa2Processor.fixStructureNamespace(document);

            assertThat(document.getDocumentCatalog().getStructureTreeRoot()).isNull();
        }
    }

    @Test
    @SneakyThrows
    void writesTheDocumentAsPdf2() {
        byte[] processed;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            documentWithParts(document, namespace());
            document.setVersion(1.6f);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            processed = PdfUa2Processor.processPdfUa2(output.toByteArray());
        }

        assertThat(new String(processed, 0, 8, StandardCharsets.US_ASCII)).isEqualTo("%PDF-2.0");
        try (PDDocument document = Loader.loadPDF(processed)) {
            PDStructureElement merged = (PDStructureElement) document.getDocumentCatalog().getStructureTreeRoot().getKids().get(0);
            assertThat(merged.getCOSObject().getDictionaryObject(NS)).as("The namespace is written").isInstanceOf(COSDictionary.class);
        }
    }

    @Test
    @SneakyThrows
    void leavesARootWithoutADocument() {
        try (PDDocument document = new PDDocument()) {
            PDStructureTreeRoot root = new PDStructureTreeRoot();
            document.getDocumentCatalog().setStructureTreeRoot(root);
            root.appendKid(new PDStructureElement("Part", root));

            PdfUa2Processor.fixStructureNamespace(document);

            assertThat(root.getCOSObject().containsKey(NAMESPACES)).isFalse();
        }
    }

    /** Of two Document elements, the one without the namespace gets it, the other keeps its own, and the root lists it once. */
    @Test
    @SneakyThrows
    void givesTheNamespaceToTheDocumentWithoutAndKeepsTheRootList() {
        try (PDDocument document = new PDDocument()) {
            COSDictionary namespace = namespace();
            PDStructureElement merged = documentWithParts(document, namespace);
            PDStructureTreeRoot root = document.getDocumentCatalog().getStructureTreeRoot();
            PDStructureElement named = new PDStructureElement("Document", root);
            named.getCOSObject().setItem(NS, namespace);
            root.appendKid(named);
            COSArray namespaces = new COSArray();
            namespaces.add(namespace);
            root.getCOSObject().setItem(NAMESPACES, namespaces);

            PdfUa2Processor.fixStructureNamespace(document);

            assertThat(merged.getCOSObject().getItem(NS)).isSameAs(namespace);
            assertThat(named.getCOSObject().getItem(NS)).isSameAs(namespace);
            assertThat(root.getCOSObject().getCOSArray(NAMESPACES).size()).as("The namespace already listed is not listed again").isEqualTo(1);
        }
    }

    /** An element reached twice, marked content and a namespace of another standard are passed over in the search. */
    @Test
    @SneakyThrows
    void searchesPastWhatHoldsNoPdf2Namespace() {
        try (PDDocument document = new PDDocument()) {
            PDStructureElement merged = documentWithParts(document, null);
            COSDictionary other = new COSDictionary();
            other.setItem(COSName.TYPE, COSName.getPDFName("Namespace"));
            other.setString(NS, "http://www.w3.org/1999/xhtml");
            COSArray kids = merged.getCOSObject().getCOSArray(COSName.K);
            COSDictionary firstPart = (COSDictionary) kids.getObject(0);
            firstPart.setItem(NS, other);
            COSArray partKids = new COSArray();
            partKids.add(COSInteger.get(0));
            COSDictionary markedContentReference = new COSDictionary();
            markedContentReference.setItem(COSName.TYPE, COSName.getPDFName("MCR"));
            markedContentReference.setInt(COSName.getPDFName("MCID"), 1);
            partKids.add(markedContentReference);
            firstPart.setItem(COSName.K, partKids);
            kids.add(firstPart);

            PdfUa2Processor.fixStructureNamespace(document);

            COSBase namespace = merged.getCOSObject().getItem(NS);
            assertThat(namespace).as("A namespace of another standard is not taken").isNotSameAs(other);
            assertThat(((COSDictionary) namespace).getString(NS)).isEqualTo(PDF_2_NAMESPACE);
        }
    }

    @Test
    @SneakyThrows
    void addsTheRevisionOfPdfUa2() {
        String fixed = PdfUa2Processor.fixXmpMetadataXml(metadata(null));

        assertThat(fixed).contains("pdfuaid:rev=\"2024\"").startsWith("<?xpacket");
    }

    @Test
    @SneakyThrows
    void correctsARevisionWhichIsNoYear() {
        assertThat(PdfUa2Processor.fixXmpMetadataXml(metadata("1"))).contains("pdfuaid:rev=\"2024\"").doesNotContain("pdfuaid:rev=\"1\"");
        assertThat(PdfUa2Processor.fixXmpMetadataXml(metadata(""))).contains("pdfuaid:rev=\"2024\"");
    }

    @Test
    @SneakyThrows
    void leavesMetadataWhichNeedsNothing() {
        String correct = metadata("2024");
        String otherPart = metadata(null).replace("pdfuaid:part=\"2\"", "pdfuaid:part=\"1\"");

        assertThat(PdfUa2Processor.fixXmpMetadataXml(correct)).isSameAs(correct);
        assertThat(PdfUa2Processor.fixXmpMetadataXml(otherPart)).as("PDF/UA-1 is not PDF/UA-2").isSameAs(otherPart);
    }

    @Test
    @SneakyThrows
    void fixesTheMetadataOfADocument() {
        try (PDDocument document = new PDDocument()) {
            PdfUa2Processor.fixXmpMetadata(document);
            assertThat(document.getDocumentCatalog().getMetadata()).as("A document without metadata stays without").isNull();

            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(metadata(null).getBytes(StandardCharsets.UTF_8));
            document.getDocumentCatalog().setMetadata(metadata);
            PdfUa2Processor.fixXmpMetadata(document);
            assertThat(new String(document.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8)).contains("pdfuaid:rev=\"2024\"");

            String fixed = new String(document.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            PdfUa2Processor.fixXmpMetadata(document);
            assertThat(new String(document.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8)).as("Fixed metadata is left as it is").isEqualTo(fixed);
        }
    }

    @Test
    @SneakyThrows
    void failsOnMalformedMetadata() {
        try (PDDocument document = new PDDocument()) {
            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata("<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n<invalid>broken xml<</invalid>\n<?xpacket end=\"r\"?>".getBytes(StandardCharsets.UTF_8));
            document.getDocumentCatalog().setMetadata(metadata);

            assertThatThrownBy(() -> PdfUa2Processor.fixXmpMetadata(document)).isInstanceOf(IOException.class).hasMessageContaining("Failed to process XMP metadata");
        }
    }

    /** The XMP metadata WeasyPrint writes for PDF/UA-2, with the given pdfuaid:rev, or none. */
    private static String metadata(String revision) {
        return "<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n"
                + "<rdf:RDF xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\" xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\" "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"
                + "<rdf:Description rdf:about=\"\" pdfuaid:part=\"2\"" + (revision != null ? " pdfuaid:rev=\"" + revision + "\"" : "") + " />"
                + "<rdf:Description rdf:about=\"\" pdf:Producer=\"WeasyPrint 70.0\" />"
                + "</rdf:RDF>\n"
                + "<?xpacket end=\"r\"?>";
    }

    private static COSDictionary namespace() {
        COSDictionary namespace = new COSDictionary();
        namespace.setItem(COSName.TYPE, COSName.getPDFName("Namespace"));
        namespace.setString(NS, PDF_2_NAMESPACE);
        return namespace;
    }

    /** A Document element as PDFBox makes of a merge, holding two parts which name the given namespace, if any. */
    private static PDStructureElement documentWithParts(PDDocument document, COSDictionary namespace) {
        PDStructureTreeRoot root = new PDStructureTreeRoot();
        document.getDocumentCatalog().setStructureTreeRoot(root);
        PDStructureElement merged = new PDStructureElement("Document", root);
        root.appendKid(merged);
        for (int index = 0; index < 2; index++) {
            PDStructureElement part = new PDStructureElement("Part", merged);
            if (namespace != null) {
                part.getCOSObject().setItem(NS, namespace);
            }
            merged.appendKid(part);
        }
        return merged;
    }
}
