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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * A header and footer with a different first page prints its first page parts on the first page, and its other parts on
 * every page after it.
 */
class FirstPageHeaderFooterTest extends BasePdfConverterTest {

    private static final String CONTENT = """
            <div style="break-after: page">Page one</div>
            <div style="break-after: page">Page two</div>
            <div>Page three</div>
            """;

    @Test
    void printsTheFirstPagePartsOnTheFirstPageOnly() {
        mockHeaderFooter(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("Running left").headerCenter("Running center").headerRight("Running right")
                .footerLeft("Running foot left").footerCenter("Running foot center").footerRight("Running page {{ PAGE_NUMBER }}")
                .differentFirstPage(true)
                .firstPageHeaderLeft("Title left").firstPageHeaderCenter("Title center").firstPageHeaderRight("Title right")
                .firstPageFooterLeft("Title foot left").firstPageFooterCenter("Title foot center").firstPageFooterRight("Title page {{ PAGE_NUMBER }}")
                .build());

        List<String> pages = pageTexts(export());

        assertThat(pages).hasSize(3);
        assertThat(pages.getFirst()).contains("Title left", "Title center", "Title right", "Title foot left", "Title foot center", "Title page 1")
                .doesNotContain("Running");
        for (int page = 1; page < pages.size(); page++) {
            assertThat(pages.get(page)).contains("Running left", "Running center", "Running right", "Running foot left", "Running foot center", "Running page " + (page + 1))
                    .doesNotContain("Title");
        }
    }

    /** A cover page takes the place of a page rendered before the document, so the first page is the one after it. */
    @Test
    void printsTheFirstPagePartsOnThePageAfterTheCoverPage() {
        mockHeaderFooter(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("Running left").headerCenter("Running center").headerRight("Running right")
                .footerLeft("Running foot left").footerCenter("Running foot center").footerRight("Running foot right")
                .differentFirstPage(true)
                .firstPageHeaderLeft("Title left").firstPageHeaderCenter("Title center").firstPageHeaderRight("Title right")
                .firstPageFooterLeft("Title foot left").firstPageFooterCenter("Title foot center").firstPageFooterRight("Title foot right")
                .build());

        List<String> pages = pageTexts(export("test"));

        assertThat(pages).hasSize(4);
        assertThat(pages.getFirst()).contains("Cover Page Title").doesNotContain("Running", "Title left");
        assertThat(pages.get(1)).contains("Page one", "Title left", "Title center", "Title right", "Title foot left", "Title foot center", "Title foot right")
                .doesNotContain("Running");
        for (int page = 2; page < pages.size(); page++) {
            assertThat(pages.get(page)).contains("Running left", "Running center", "Running right", "Running foot left", "Running foot center", "Running foot right")
                    .doesNotContain("Title");
        }
    }

    @Test
    void leavesTheFirstPageBareWhenItsPartsAreEmpty() {
        mockHeaderFooter(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("Running left").headerCenter("Running center").headerRight("Running right")
                .footerLeft("Running foot left").footerCenter("Running foot center").footerRight("Running foot right")
                .differentFirstPage(true)
                .build());

        List<String> pages = pageTexts(export());

        assertThat(pages.getFirst()).contains("Page one").doesNotContain("Running");
        assertThat(pages.get(1)).contains("Running left", "Running foot right");
    }

    @Test
    void ignoresTheFirstPagePartsWhenTheFirstPageIsNotDifferent() {
        mockHeaderFooter(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("Running left").headerCenter("Running center").headerRight("Running right")
                .footerLeft("Running foot left").footerCenter("Running foot center").footerRight("Running foot right")
                .differentFirstPage(false)
                .firstPageHeaderLeft("Title left")
                .build());

        List<String> pages = pageTexts(export());

        assertThat(pages.getFirst()).contains("Running left", "Running foot right").doesNotContain("Title");
    }

    private void mockHeaderFooter(@NotNull HeaderFooterModel model) {
        when(headerFooterSettings.load(any(), any())).thenReturn(model);
    }

    private byte @NotNull [] export() {
        return export(null);
    }

    private byte @NotNull [] export(@Nullable String coverPage) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .coverPage(coverPage)
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A document with a different first page")
                .content(CONTENT)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        return converter.convertToPdf(params, null);
    }

    /** The text of each page, in page order. */
    @SneakyThrows
    private @NotNull List<String> pageTexts(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<String> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return pages;
        }
    }
}
