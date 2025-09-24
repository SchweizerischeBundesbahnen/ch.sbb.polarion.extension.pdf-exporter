package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PdfConverterWeasyPrintWatermarkTest extends BasePdfConverterTest {

    @Override
    protected void prepareSpecificMocks() {
        // No specific mocks needed for watermark tests
    }

    @Override
    protected void setupHeaderFooterSettings() {
        // Custom header/footer settings for watermark tests
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("HL")
                .headerCenter("HC")
                .headerRight("HR")
                .footerLeft("FL")
                .footerCenter("FC {{ PAGE_NUMBER }}")
                .footerRight("FR")
                .build());
    }

    @Test
    void testConverterWithWatermark() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Watermarked Document")
                .content("<h1>Confidential Document</h1><p>This document should have a watermark.</p><p>The watermark should say 'Confidential' and be visible across the page.</p>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterWithoutWatermark() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(false)  // Disable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Regular Document")
                .content("<h1>Regular Document</h1><p>This document should NOT have a watermark.</p>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterWithWatermarkMultiplePages() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Multi-page Watermarked Document")
                .content("""
                        <h1>Page 1</h1>
                        <p>First page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 2</h1>
                        <p>Second page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 3</h1>
                        <p>Third page content with watermark.</p>
                        """)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    @SneakyThrows
    void testConverterWithWatermarkAndCoverPage() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .watermark(true)  // Enable watermark
                .coverPage("test")  // Enable cover page
                .build();

        Path tempFile = Files.createTempFile("attachment", ".pdf");
        Files.writeString(tempFile, "test attachment content");

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Document with Cover and Watermark")
                .content("""
                        <h1>Page 1</h1>
                        <p>First page content with watermark.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 2</h1>
                        <p>Second page content with watermark.</p>
                        """)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .attachmentFiles(List.of(tempFile))
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

}
