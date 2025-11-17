package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant.PDF_A_3B;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
class PdfAttachmentsTest extends BaseWeasyPrintTest {

    @Mock
    private IModule module;

    @Test
    @SneakyThrows
    void testAttachedFiles() {
        String testName = getCurrentMethodName();

        List<Path> attachments = new ArrayList<>();

        URL resource = getClass().getResource("/test_img.png");
        if (resource == null) {
            throw new IllegalArgumentException("Test file not found");
        }
        File file = new File(resource.toURI());

        attachments.add(file.toPath());

        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testDocument")
                .content("test document content")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .attachmentFiles(attachments)
                .build();

        byte[] documentBytes = exportToPdf("""
                <html>
                <body>
                    <p>Some document</p>
                </body>
                </html>
                """, WeasyPrintOptions.builder().pdfVariant(PDF_A_3B).build(), documentData); // pdf/a-3b is needed to handle attachments properly

        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());

            PDEmbeddedFilesNameTreeNode embeddedFiles = namesDictionary.getEmbeddedFiles();
            assertNotNull(embeddedFiles);

            Map<String, PDComplexFileSpecification> embeddedFilesMap = embeddedFiles.getNames();
            assertNotNull(embeddedFilesMap);
            assertEquals(1, embeddedFilesMap.size());

            String embeddedFileName = embeddedFilesMap.keySet().iterator().next();
            assertEquals("test_img.png", embeddedFileName);

            PDEmbeddedFile embeddedFile = embeddedFilesMap.get(embeddedFileName).getEmbeddedFile();
            assertNotNull(embeddedFile);

            try (InputStream embeddedFileInputStream = embeddedFile.createInputStream();
                 InputStream attachmentInputStream = getClass().getResourceAsStream("/test_img.png")) {
                assertTrue(IOUtils.contentEquals(embeddedFileInputStream, attachmentInputStream));
            }
        }

        writeReportPdf(testName, "content", documentBytes);
    }

    @Test
    @SneakyThrows
    void testNoAttachments() {
        String testName = getCurrentMethodName();

        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testDocument")
                .content("test document content")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .build();

        byte[] documentBytes = exportToPdf("""
                <html>
                <body>
                    <p>Some document</p>
                </body>
                </html>
                """, WeasyPrintOptions.builder().pdfVariant(PDF_A_3B).build(), documentData); // pdf/a-3b is needed to handle attachments properly

        try (PDDocument document = Loader.loadPDF(documentBytes)) {
            PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());

            PDEmbeddedFilesNameTreeNode embeddedFiles = namesDictionary.getEmbeddedFiles();
            assertNull(embeddedFiles);
        }

        writeReportPdf(testName, "content", documentBytes);
    }
}
