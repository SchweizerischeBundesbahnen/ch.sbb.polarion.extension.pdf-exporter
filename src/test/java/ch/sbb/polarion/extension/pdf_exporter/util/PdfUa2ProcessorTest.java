package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

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
