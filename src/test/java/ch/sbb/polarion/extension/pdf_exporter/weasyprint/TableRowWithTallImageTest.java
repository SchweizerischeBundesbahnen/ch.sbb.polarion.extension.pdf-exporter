package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * A table row is not split across pages where it holds an image.
 * <p>
 * Half a page of text leaves the row too little room, so it moves to the next page. Splitting it left the
 * image behind and the header of the table above a row which showed nothing.
 * </p>
 */
class TableRowWithTallImageTest extends BasePdfConverterTest {

    /** A diagram of its own 300x3000 px, which fit to page shortens to the height of a page. */
    private static final String DIAGRAM = "/weasyprint/img/tall-chain.svg";

    /** A thumbnail of its own 200x100 px, in the shape its image states. */
    private static final String THUMBNAIL = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(("""
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100">
              <rect x="2" y="2" width="196" height="96" fill="#e8f0fe" stroke="#1a73e8" stroke-width="3"/>
            </svg>""").getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** The text fills the first page, and the table takes the second one whole. */
    private static final int DOCUMENT_PAGES = 2;

    /** The shape of the diagram, 300 x 3000 px, which it keeps however short a header leaves it. */
    private static final double DIAGRAM_RATIO = 300d / 3000d;

    @Test
    void keepsATableRowWhichHoldsAnImageWhole() {
        export("tableRowWithTallImage", getCurrentMethodName(), "Diagram 1", true);
    }

    /** However many rows a header takes, the image gives up the height it needs. */
    @Test
    void keepsATableRowUnderATwoRowHeader() {
        export("tableRowWithTallImageUnderTwoHeaderRows", getCurrentMethodName(), "Diagram 1", true);
    }

    /** A header of one row can still take three lines of it, and the image gives up that height too. */
    @Test
    void keepsATableRowUnderAWrappedHeader() {
        export("tableRowWithTallImageUnderAWrappedHeader", getCurrentMethodName(), "A header which states", true);
    }

    /** A caption under the image in its cell takes room of the page too, and the image gives that height up. */
    @Test
    void keepsATableRowWithAnImageAndItsCaptionWhole() {
        export("tableRowWithTallImageAndCaption", getCurrentMethodName(), "Diagram 1", true);
    }

    /** A style package which makes the header taller leaves the row the less of the page. */
    @Test
    void keepsATableRowWholeUnderAHeaderTheCssMakesTaller() {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(readFontCss() + "th { font-size: 20pt; padding: 24px !important; }")
                .build());
        export("tableRowWithTallImage", getCurrentMethodName(), "Diagram 1", true);
    }

    /** A column of a landscape page is wider than a diagram may grow, and the row is kept whole all the same. */
    @Test
    void keepsATableRowWithAnImageInAWideColumnWhole() {
        export("tableRowWithTallImageInAWideColumn", getCurrentMethodName(), "Diagram 1", Orientation.LANDSCAPE, true);
    }

    /** An image which states its size can be as tall as a page too, and its row is kept whole as well. */
    @Test
    void keepsATableRowWithAnImageOfAStatedSizeWhole() {
        export("tableRowWithTallImageOfAStatedSize", getCurrentMethodName(), "Diagram 1", true);
    }

    private void export(@NotNull String resource, @NotNull String testName, @NotNull String headerWords, boolean compareWithReferences) {
        export(resource, testName, headerWords, Orientation.PORTRAIT, compareWithReferences);
    }

    /** A height stated in points is as tall as one stated in pixels, and the row is kept whole all the same. */
    @Test
    void keepsATableRowWithAnImageOfAHeightInPointsWhole() {
        export("tableRowWithTallImageInPoints", getCurrentMethodName(), "Diagram 1", true);
    }

    /** The table nested in a cell of another one keeps its row with its own header. */
    @Test
    void keepsATableRowOfANestedTableWhole() {
        export("tableRowWithTallImageInANestedTable", getCurrentMethodName(), "Diagram 1", true);
    }

    /** Text of another cell which runs over a page splits the row anyway, so it starts where it stands, and leaves no page blank. */
    @Test
    void letsARowWhoseTextRunsOverAPageStartUnderTheText() {
        byte[] pdf = pdfOf("tableRowWithTallImageAndALongNote", Orientation.PORTRAIT);
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        assertEquals(0, pagesWhichCarry(pdf, "line 1.").getFirst(), "The row starts on the first page, under the text above the table");
        assertTrue(pagesWhichCarry(pdf, "line 120.").getFirst() > 0, "Its note runs on to the pages after it");
        assertFalse(differ, "The pages differ from the reference images");
    }

    /** The column narrows an image which states a shape taller than the page, and its row is kept whole. */
    @Test
    void keepsATableRowWithAnImageTheColumnNarrowsWhole() {
        export("tableRowWithAWideImage", getCurrentMethodName(), "Diagram 1", true);
    }

    /** A row which lets itself break is kept whole all the same: its image does not split, and the table moves on whole. */
    @Test
    void keepsARowWhichLetsItselfBreakWhole() {
        export("tableRowWithTallImageWhichStatesHowItBreaks", getCurrentMethodName(), "Diagram 1", true);
    }

    /** A thumbnail the column narrows stays low, so its row breaks where it must and the table stays under the text. */
    @Test
    void leavesATableWithAThumbnailTheColumnNarrowsWhereItIs() {
        byte[] pdf = pdfOf("tableRowWithAThumbnail", Orientation.PORTRAIT);
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        assertEquals(0, pagesWhichCarry(pdf, "Thumbnail 1").getFirst(), "The table starts under the text, where it stands, and leaves no page blank");
        assertFalse(differ, "The pages differ from the reference images");
    }

    private void export(@NotNull String resource, @NotNull String testName, @NotNull String headerWords, @NotNull Orientation orientation, boolean compareWithReferences) {
        byte[] pdf = pdfOf(resource, orientation);

        assertEquals(DOCUMENT_PAGES, pageCount(pdf), "The text fills the first page and the table takes the second, whole");
        assertEquals(List.of(DOCUMENT_PAGES - 1), pagesWhichCarry(pdf, headerWords),
                "The header belongs to the page its row is on, and a header left on the page before heads nothing there");
        List<List<Integer>> drawn = DrawnImages.sizesIn(pdf);
        assertEquals(1, drawn.size(), "The document draws the diagram once");
        assertEquals(DIAGRAM_RATIO, (double) drawn.getFirst().getFirst() / drawn.getFirst().getLast(), 0.01d,
                "The diagram is drawn whole, in the shape of the file it comes from");
        if (compareWithReferences) {
            assertFalse(compareContentUsingReferenceImages(testName, pdf), "The pages differ from the reference images");
        }
    }

    private byte @NotNull [] pdfOf(@NotNull String resource, @NotNull Orientation orientation) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(orientation)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A table row which holds an image")
                .content(readHtmlResource(resource).replace("{DIAGRAM}", diagramSource()).replace("{THUMBNAIL}", THUMBNAIL))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        return converter.convertToPdf(params, null);
    }

    /** The pages whose text holds the given words, counted from zero. */
    @SneakyThrows
    private @NotNull List<Integer> pagesWhichCarry(byte @NotNull [] pdf, @NotNull String words) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<Integer> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                if (stripper.getText(document).contains(words)) {
                    pages.add(page - 1);
                }
            }
            return pages;
        }
    }

    @SneakyThrows
    private int pageCount(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    @SneakyThrows
    private @NotNull String diagramSource() {
        try (InputStream resource = TableRowWithTallImageTest.class.getResourceAsStream(DIAGRAM)) {
            byte[] svg = Objects.requireNonNull(resource, DIAGRAM).readAllBytes();
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg);
        }
    }
}
