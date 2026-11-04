package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * A page break the document ends with leaves no page behind.
 * <p>
 * The editor sets the orientation of the page above a break, so a document which ends in landscape carries a
 * break after its last chapter. The export used to give that break a page of its own, blank and in the
 * orientation of the document - reported in #1084.
 * </p>
 */
class PageBreakAtTheEndTest extends BasePdfConverterTest {

    @Test
    void leavesNoPageAfterThePageBreakADocumentEndsWith() {
        export("pageBreakAtTheEnd", getCurrentMethodName(), 1);
    }

    /** A break between two chapters is what a break is for, and it is untouched. */
    @Test
    void givesAPageToEachChapterAPageBreakSeparates() {
        export("pageBreakInTheMiddle", getCurrentMethodName(), 2);
    }

    /** Two breaks in a row leave a page blank between two chapters, which is the page the document asks for. */
    @Test
    void keepsThePageTwoBreaksInARowLeaveBlank() {
        export("pageBreakLeavingAPageEmpty", getCurrentMethodName(), 3);
    }

    /** An empty table, an empty list and spaces with a background show on the page: they are the user's, not an empty page. */
    @Test
    void keepsThePageOfWhatShowsWithoutTextAfterALastPageBreak() {
        export("pageBreakBeforeWhatShows", getCurrentMethodName(), 2);
    }

    /** Blank paragraphs between two breaks are a blank page the user asks for, also when empty chapters are cut. */
    @Test
    void keepsThePageOfBlankParagraphsWhenEmptyChaptersAreCut() {
        export("pageBreakLeavingAPageEmptyBeforeALine", getCurrentMethodName(), 4, true);
    }

    private void export(@NotNull String resource, @NotNull String testName, int expectedPages) {
        export(resource, testName, expectedPages, false);
    }

    private void export(@NotNull String resource, @NotNull String testName, int expectedPages, boolean cutEmptyChapters) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .cutEmptyChapters(cutEmptyChapters)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A page break at the end")
                .content(readHtmlResource(resource))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        byte[] pdf = converter.convertToPdf(params, null);

        assertEquals(expectedPages, pageCount(pdf), "The pages a document of breaks runs to");
        assertFalse(compareContentUsingReferenceImages(testName, pdf), "The pages differ from the reference images");
    }

    @SneakyThrows
    private int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }
}
