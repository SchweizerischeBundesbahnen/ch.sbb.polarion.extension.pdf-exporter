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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * A table row is not split across pages.
 * <p>
 * Split, a row leaves its first cells on one page and the rest of it on the next: an ID above nothing, and a title
 * below no ID. The table here has enough rows of two lines that one of them meets the end of a page.
 * </p>
 */
class TableRowKeptWholeTest extends BasePdfConverterTest {

    private static final int ROWS = 40;

    /** The icon of a work item type, 16 pixels square as Polarion draws it. */
    private static final String ICON = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(
            "<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'><rect width='16' height='16' fill='#c00'/></svg>".getBytes(StandardCharsets.UTF_8));

    /** Room above the table, as much as it takes for the end of the first page to fall inside a row. */
    private static final String PREFACE = "<div style=\"height: 3px\"></div>";

    @Test
    void keepsEveryRowOfATableOnOnePage() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A table of rows which take two lines each")
                .content(content())
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        byte[] pdf = converter.convertToPdf(params, null);
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(2);
        for (int row = 1; row <= ROWS; row++) {
            assertThat(pageOf(pages, "S" + row + "S")).as("Row %d starts and ends on one page", row).isEqualTo(pageOf(pages, "E" + row + "E"));
        }
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /**
     * The table of a Live Report, as Polarion renders a query of work items: the ID cell holds an inline block with
     * the icon of the type, the title cell plain text which takes two lines.
     */
    private static @NotNull String content() {
        String rows = IntStream.rangeClosed(1, ROWS)
                .mapToObj(row -> """
                        <tr class="polarion-rpw-table-content-row">
                          <td>
                            <div style="display:inline-block;white-space:nowrap;">
                              <span class="polarion-no-style-cleanup" style="white-space:nowrap;"><a style="font-size:1em;" class="polarion-Hyperlink" href="#"><span style="white-space:nowrap;"><img src="%s" class="polarion-Icons" style="max-height:837px;
                        object-fit:contain;" /></span><span style="color:#000000;">S%dS</span></a></span>
                            </div>
                          </td>
                          <td>A title long enough to take a second line in the column it is given, which is where a page break could fall E%dE</td>
                        </tr>
                        """.formatted(ICON, row, row))
                .collect(Collectors.joining());
        return PREFACE + "<table class=\"polarion-rpw-table-content\"><tbody>" + rows + "</tbody></table>";
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
