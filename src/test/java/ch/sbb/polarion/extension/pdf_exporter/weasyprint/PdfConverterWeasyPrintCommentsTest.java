package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

class PdfConverterWeasyPrintCommentsTest extends BasePdfConverterTest {

    @Test
    void testConverterWithRenderedComments() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .renderComments(CommentsRenderType.ALL)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Test Document")
                .content("""
                        <p>This document should have a comment here:
                            [span class=comment level-0]
                                [span class=meta]
                                    [span class=date]2025-03-24 09:36[/span]
                                    [span class=details]
                                        [span class=author]Test User 1[/span]
                                    [/span]
                                [/span]
                                [span class=text]Test Comment[/span]
                            [/span]
                            [span class=comment level-1]
                                [span class=meta]
                                    [span class=date]2025-05-27 13:46[/span]
                                    [span class=details]
                                        [span class=author]Test User 1[/span]
                                    [/span]
                                [/span]
                                [span class=text]Test Reply[/span]
                            [/span]
                        </p>""")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterWithNativeComments() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .renderComments(CommentsRenderType.OPEN)
                .renderNativeComments(true)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Test Document")
                .content("""
                        <p>This document should have a comment here:
                            [span class=sticky-note]
                                [span class=sticky-note-time]2020-04-30T08:30:00+08:00[/span]
                                [span class=sticky-note-username]Test User[/span]
                                [span class=sticky-note-text]Comment body[/span]
                            [/span]
                        </p>""")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();

        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        // Act & Assert
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

}
