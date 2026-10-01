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
 * A work item whose presentation asks for "No Page Break" is not split across two pages.
 * <p>
 * Polarion wraps such a work item into a one-cell table with "page-break-inside: avoid". The work item here has no
 * table of its own and is placed so that the end of the first page falls inside it.
 * </p>
 */
class NoPageBreakWorkItemTest extends BasePdfConverterTest {

    /**
     * Content above the work item, as much as it takes for the end of the first page to fall inside it. Framed, so that
     * the pages show where it ends and the work item would have started.
     */
    private static final String PREFACE = "<div style=\"height: 718px; border: 1px dashed #999;\">Content above the work item</div>";

    /** The lines of the work item, each marked with its number. */
    private static final int LINES = 12;

    @Test
    void keepsTheWorkItemOnOnePage() {
        byte[] pdf = export();
        List<String> pages = pageTexts(pdf);

        assertThat(pages).as("The work item moves whole to the second page").hasSize(2);
        assertThat(pageOf(pages, "L1L")).as("The work item is printed").isNotNegative();
        assertThat(pageOf(pages, "L1L")).as("The work item starts and ends on one page").isEqualTo(pageOf(pages, "L" + LINES + "L"));
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
                .title("No Page Break")
                .content(PREFACE + readHtmlResource("noPageBreakWorkItem"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        return converter.convertToPdf(params, null);
    }

    /** The page, counted from zero, whose text holds the given words, however the lines wrapped them. */
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
