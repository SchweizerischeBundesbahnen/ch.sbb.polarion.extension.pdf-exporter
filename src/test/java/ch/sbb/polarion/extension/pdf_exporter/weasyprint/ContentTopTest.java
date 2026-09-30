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
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

/**
 * The content of the first page starts as high as on every page after it.
 * <p>
 * The reference images compare each page with a picture of itself, so a refreshed picture could take an offset of the
 * first page back in. This test compares the pages with each other instead.
 * </p>
 */
class ContentTopTest extends BasePdfConverterTest {

    private static final String CONTENT = """
            <div style="break-after: page">Page one</div>
            <div style="break-after: page">Page two</div>
            <div>Page three</div>
            """;

    @Test
    void startsTheContentOfEveryPageAtTheSameHeight() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("A document of three pages")
                .content(CONTENT)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        List<Float> tops = contentTops(converter.convertToPdf(params, null), List.of("Page one", "Page two", "Page three"));

        assertThat(tops).hasSize(3);
        assertThat(tops.get(0)).as("The first page starts as high as the second one").isCloseTo(tops.get(1), within(0.5f));
        assertThat(tops.get(2)).as("The third page starts as high as the second one").isCloseTo(tops.get(1), within(0.5f));
    }

    /** The top of the given line on each page, in the order of the pages. */
    @SneakyThrows
    private @NotNull List<Float> contentTops(byte @NotNull [] pdf, @NotNull List<String> lines) {
        List<Float> tops = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                String line = lines.get(page - 1);
                float[] top = {Float.NaN};
                PDFTextStripper stripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String text, List<TextPosition> positions) {
                        if (text.contains(line) && Float.isNaN(top[0])) {
                            top[0] = positions.getFirst().getYDirAdj() - positions.getFirst().getHeightDir();
                        }
                    }
                };
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                stripper.getText(document);
                assertThat(top[0]).as("Page %d carries \"%s\"", page, line).isNotNaN();
                tops.add(top[0]);
            }
        }
        return tops;
    }
}
