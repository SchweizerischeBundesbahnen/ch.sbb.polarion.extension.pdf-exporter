package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.generic.settings.SettingId;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * A style package may name a header and footer of the first page. Its parts are printed on the first page, and the parts
 * of the header and footer of the style package on every page after it.
 */
class FirstPageHeaderFooterTest extends BasePdfConverterTest {

    private static final String CONTENT = """
            <div style="break-after: page">Page one</div>
            <div style="break-after: page">Page two</div>
            <div>Page three</div>
            """;

    private static final HeaderFooterModel RUNNING = HeaderFooterModel.builder()
            .useCustomValues(true)
            .headerLeft("Running left").headerCenter("Running center").headerRight("Running right")
            .footerLeft("Running foot left").footerCenter("Running foot center").footerRight("Running page {{ PAGE_NUMBER }}")
            .build();

    private static final HeaderFooterModel TITLE = HeaderFooterModel.builder()
            .useCustomValues(true)
            .headerLeft("Title left").headerCenter("Title center").headerRight("Title right")
            .footerLeft("Title foot left").footerCenter("Title foot center").footerRight("Title page {{ PAGE_NUMBER }}")
            .build();

    @Test
    void printsTheFirstPagePartsOnTheFirstPageOnly() {
        mockHeaderFooters(TITLE);

        byte[] pdf = export(null, "title");
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(3);
        assertThat(pages.getFirst()).contains("Title left", "Title center", "Title right", "Title foot left", "Title foot center", "Title page 1")
                .doesNotContain("Running");
        for (int page = 1; page < pages.size(); page++) {
            assertThat(pages.get(page)).contains("Running left", "Running center", "Running right", "Running foot left", "Running foot center", "Running page " + (page + 1))
                    .doesNotContain("Title");
        }
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /** A cover page takes the place of a page rendered before the document, so the first page is the one after it. */
    @Test
    void printsTheFirstPagePartsOnThePageAfterTheCoverPage() {
        mockHeaderFooters(TITLE);

        byte[] pdf = export("test", "title");
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(4);
        assertThat(pages.getFirst()).contains("Cover Page Title").doesNotContain("Running", "Title left");
        assertThat(pages.get(1)).contains("Page one", "Title left", "Title center", "Title right", "Title foot left", "Title foot center", "Title page 2")
                .doesNotContain("Running");
        for (int page = 2; page < pages.size(); page++) {
            assertThat(pages.get(page)).contains("Running left", "Running center", "Running right", "Running foot left", "Running foot center", "Running page " + (page + 1))
                    .doesNotContain("Title");
        }
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    void leavesTheFirstPageBareWhenItsHeaderAndFooterIsEmpty() {
        mockHeaderFooters(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("").headerCenter("").headerRight("")
                .footerLeft("").footerCenter("").footerRight("")
                .build());

        byte[] pdf = export(null, "title");
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(3);
        assertThat(pages.getFirst()).contains("Page one").doesNotContain("Running");
        assertThat(pages.get(1)).contains("Running left", "Running page 2");
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    @Test
    void printsTheOtherPartsOnTheFirstPageWithoutAHeaderAndFooterOfItsOwn() {
        mockHeaderFooters(null);

        byte[] pdf = export(null, null);
        List<String> pages = pageTexts(pdf);

        assertThat(pages).hasSize(3);
        assertThat(pages.getFirst()).contains("Running left", "Running page 1").doesNotContain("Title");
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    /** The style package names "running" for the other pages and "title" for the first one, if it has one. */
    private void mockHeaderFooters(@Nullable HeaderFooterModel title) {
        when(headerFooterSettings.load(any(), eq(SettingId.fromName("running")))).thenReturn(RUNNING);
        if (title != null) {
            when(headerFooterSettings.load(any(), eq(SettingId.fromName("title")))).thenReturn(title);
        }
    }

    private byte @NotNull [] export(@Nullable String coverPage, @Nullable String firstPageHeaderFooter) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .coverPage(coverPage)
                .headerFooter("running")
                .firstPageHeaderFooter(firstPageHeaderFooter)
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
