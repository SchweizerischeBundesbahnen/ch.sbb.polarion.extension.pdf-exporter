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
 * A row of a document table which fits a page is not split across two.
 * <p>
 * The table is the one of user accounts of the Product Specification of the E-Library project, in the markup Polarion
 * renders, placed so the end of the first page falls inside the row of the Patron. Split, that row left "Patron" at the
 * bottom of one page and what a patron may do at the top of the next, in a row without its account type.
 * </p>
 */
class DocumentTableRowKeptWholeTest extends BasePdfConverterTest {

    /** Room above the table, as much as it takes for the end of the first page to fall inside the row of the Patron. */
    private static final String PREFACE = "<div style=\"height: 750px\"></div>";

    @Test
    void keepsTheRowOfTheTableOnOnePage() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("User accounts")
                .content(PREFACE + readHtmlResource("documentTableOfAccountTypes"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        byte[] pdf = converter.convertToPdf(params, null);
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(2);
        assertThat(pageOf(pages, "Patron")).as("The account type and what it may do are on one page")
                .isEqualTo(pageOf(pages, "reserve, and purchase."));
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /** The page, counted from zero, whose text holds the given words. */
    private static int pageOf(@NotNull List<String> pages, @NotNull String words) {
        return IntStream.range(0, pages.size()).filter(page -> pages.get(page).contains(words)).findFirst().orElse(-1);
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
