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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * A row of a work items table in a Live Report is not split across pages, and a row of a document table still is.
 * <p>
 * Split, a row of work items leaves its first cells on one page and the rest of it on the next: an ID above nothing,
 * and a title below no ID. The table here has enough rows of two lines that one of them meets the end of a page.
 * </p>
 */
class TableRowKeptWholeTest extends BasePdfConverterTest {

    private static final int ROWS = 28;

    /** A border around each cell, so the pages show where a row ends and whether it was split. */
    private static final String CELL = "<td style=\"border: 1px solid #999;\">";

    /**
     * Enough lines for a title more than two pages tall. Short lines, as a cell of long text takes WeasyPrint minutes to
     * lay out across pages.
     */
    private static final int LONG_ROW_LINES = 120;

    /**
     * The icon of a work item type, 16 pixels square as Polarion draws it. A PNG, as Polarion's icons are bitmaps: the conversion service turns every SVG into a PNG first, which
     * takes it about half a second an image.
     */
    private static final String ICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAF0lEQVR4nGM4w8BAEiJN9aiGUQ1DSgMAQWfMAdovJBMAAAAASUVORK5CYII=";

    /** Room above the table, as much as it takes for the end of the first page to fall inside a row. */
    private static final String PREFACE = "<div style=\"height: 33px\"></div>";

    @Test
    void keepsEveryRowOfAWorkItemsTableOnOnePage() {
        byte[] pdf = export(content());
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(2);
        for (int row = 1; row <= ROWS; row++) {
            assertThat(pageOf(pages, "S" + row + "S")).as("Row %d starts and ends on one page", row).isEqualTo(pageOf(pages, "E" + row + "E"));
        }
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /**
     * A table of a document is not a table of work items: its row may be taller than a page, and it breaks where the
     * page ends. Kept whole, it would move to a page of its own and leave the page before it empty.
     */
    @Test
    void breaksARowOfADocumentTableWhereThePageEnds() {
        String lines = IntStream.rangeClosed(1, LONG_ROW_LINES)
                .mapToObj(line -> "Line L" + line + "L")
                .collect(Collectors.joining("<br/>"));
        byte[] pdf = export(PREFACE + "<table style=\"border-collapse: collapse;\"><tbody>"
                + "<tr>" + CELL + "S1S</td>" + CELL + "A row above the tall one E1E</td></tr>"
                + "<tr>" + CELL + "S2S</td>" + CELL + lines + "</td></tr>"
                + "<tr>" + CELL + "S3S</td>" + CELL + "A row below the tall one E3E</td></tr>"
                + "</tbody></table>");
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(3);
        assertThat(pageOf(pages, "L1L")).as("The tall row starts on the first page, under the row above it").isZero();
        int previousPage = 0;
        for (int line = 1; line <= LONG_ROW_LINES; line++) {
            int page = pageOf(pages, "L" + line + "L");
            assertThat(page).as("Line %d of the tall row is printed", line).isNotNegative();
            assertThat(page).as("Line %d follows the one before it", line).isGreaterThanOrEqualTo(previousPage);
            previousPage = page;
        }
        assertThat(pageOf(pages, "E3E")).as("The row below the tall one is printed").isNotNegative();
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] export(@NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A table of work items")
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        return converter.convertToPdf(params, null);
    }

    /**
     * The table of a Live Report, as Polarion renders a query of work items: the ID cell holds an inline block with
     * the icon of the type, the title cell plain text which takes two lines.
     */
    private static @NotNull String content() {
        String rows = IntStream.rangeClosed(1, ROWS)
                .mapToObj(row -> """
                        <tr class="polarion-rpw-table-content-row">
                          %s
                            <div style="display:inline-block;white-space:nowrap;">
                              <span class="polarion-no-style-cleanup" style="white-space:nowrap;"><a style="font-size:1em;" class="polarion-Hyperlink" href="#"><span style="white-space:nowrap;"><img src="%s" class="polarion-Icons" style="max-height:837px;
                        object-fit:contain;" /></span><span style="color:#000000;">S%dS</span></a></span>
                            </div>
                          </td>
                          %sA title long enough to take a second line in the column it is given, which is where a page break could fall E%dE</td>
                        </tr>
                        """.formatted(CELL, ICON, row, CELL, row))
                .collect(Collectors.joining());
        return PREFACE + "<table class=\"polarion-rpw-table-content\" style=\"border-collapse: collapse;\"><tbody>" + rows + "</tbody></table>";
    }

    /** The page, counted from zero, whose text holds the given mark, however the column wrapped it. */
    private static int pageOf(@NotNull List<String> pages, @NotNull String mark) {
        return IntStream.range(0, pages.size()).filter(page -> pages.get(page).replace(" ", "").contains(mark)).findFirst().orElse(-1);
    }

    /** The text of each page, in page order, with the lines of a cell joined into one. */
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
