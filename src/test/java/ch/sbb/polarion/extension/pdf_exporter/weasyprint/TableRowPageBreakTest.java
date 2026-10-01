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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * A table which Polarion wraps into a one-cell table with "page-break-inside: avoid" breaks between its rows, not inside
 * them, and is not moved whole to the next page.
 * <p>
 * The wrapper is unwrapped and its intent passed on to the rows of the tables inside it. The table is placed so that the
 * end of the first page falls inside it.
 * </p>
 */
class TableRowPageBreakTest extends BasePdfConverterTest {

    /**
     * Room above the table, as much as it takes for the end of the first page to fall inside the row of REQ-004 when
     * nothing keeps its rows whole.
     */
    private static final String PREFACE = "<div style=\"height: 330px\"></div>";

    /** The ID of each requirement, and the words its text ends with. */
    private static final List<List<String>> ROWS = List.of(
            List.of("REQ-001", "are clearly marked and reported to the user."),
            List.of("REQ-002", "unnecessary blank pages in the output."),
            List.of("REQ-003", "table cells for internationalization support."),
            List.of("REQ-004", "ability to select a template at export time."),
            List.of("REQ-005", "pages including the cover page when enabled."),
            List.of("REQ-006", "improved readability of the table of contents."));

    @Test
    void testTableRowPageBreakAvoid() {
        byte[] pdf = export();
        List<String> pages = pageTexts(pdf);

        assertThat(pages).as("The table runs onto a second page").hasSize(2);
        assertThat(pageOf(pages, "REQ-001")).as("The table starts on the first page, not moved whole to the next").isZero();
        for (List<String> row : ROWS) {
            assertThat(pageOf(pages, row.get(0))).as("%s is printed", row.get(0)).isNotNegative();
            assertThat(pageOf(pages, row.get(1))).as("The end of %s is printed", row.get(0)).isNotNegative();
            assertThat(pageOf(pages, row.get(0))).as("%s starts and ends on one page", row.get(0)).isEqualTo(pageOf(pages, row.get(1)));
        }
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] export() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("tableRowPageBreakTitle")
                .content(PREFACE + readHtmlResource("tableRowPageBreak"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        return converter.convertToPdf(params, null);
    }

    /** The page, counted from zero, whose text holds the given words, however the column wrapped them. */
    private static int pageOf(@NotNull List<String> pages, @NotNull String words) {
        String wanted = words.replace(" ", "");
        return IntStream.range(0, pages.size()).filter(page -> pages.get(page).replace(" ", "").contains(wanted)).findFirst().orElse(-1);
    }

    /** The text of each page, in page order, with its lines joined. */
    @SneakyThrows
    private @NotNull List<String> pageTexts(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document).replaceAll("\\s+", " "));
            }
            return pages;
        }
    }
}
