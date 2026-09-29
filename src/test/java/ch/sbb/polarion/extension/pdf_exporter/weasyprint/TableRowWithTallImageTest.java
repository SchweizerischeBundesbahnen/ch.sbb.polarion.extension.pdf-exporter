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
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

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

    /** The text fills the first page, and the table takes the second one whole. */
    private static final int DOCUMENT_PAGES = 2;

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
        // The height of such a header is measured by laying the table out, and a font is a pixel taller on one
        // machine than on another, which moves the image on the page. What the pages hold is what is read here.
        export("tableRowWithTallImageUnderAWrappedHeader", getCurrentMethodName(), "A header which states", false);
    }

    private void export(@NotNull String resource, @NotNull String testName, @NotNull String headerWords, boolean compareWithReferences) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A table row which holds an image")
                .content(readHtmlResource(resource).replace("{DIAGRAM}", diagramSource()))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        byte[] pdf = converter.convertToPdf(params, null);

        assertEquals(DOCUMENT_PAGES, pageCount(pdf), "The text fills the first page and the table takes the second, whole");
        assertEquals(List.of(DOCUMENT_PAGES - 1), pagesWhichCarry(pdf, headerWords),
                "The header belongs to the page its row is on, and a header left on the page before heads nothing there");
        if (compareWithReferences) {
            assertFalse(compareContentUsingReferenceImages(testName, pdf), "The pages differ from the reference images");
        }
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
