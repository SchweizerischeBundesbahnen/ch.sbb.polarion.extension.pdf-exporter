package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveReportId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IRichPage;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * A chart of a Live Report fits its column. Highcharts draws it as an inline SVG at the width of the page it was drawn on,
 * here 690 px in a column of 70%, which is narrower. Drawn at that width, the chart reached into the column beside it and
 * its white background covered the parameters of the query there; scaled to the column at its own height, it was
 * squeezed. It is scaled down to its column and keeps its own shape, whichever renderer of WeasyPrint service draws it.
 * <p>
 * A Live Report localizes no enumerations, so the localization the base class stubs for a document goes unused.
 * </p>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class LiveReportChartTest extends BasePdfConverterTest {

    /** The shape of the chart, 690 x 300 px, which its viewBox states. */
    private static final double CHART_RATIO = 690d / 300d;

    @Test
    void fitsAChartWiderThanItsColumnToTheColumn() {
        byte[] pdf = exportLiveReport();
        writeReportPdf(getCurrentMethodName(), "generated", pdf);

        List<DrawnImages.Box> charts = DrawnImages.boxesIn(pdf);
        assertThat(charts).as("The report draws its chart once").hasSize(1);
        DrawnImages.Box chart = charts.getFirst();
        assertThat(chart.left() + chart.width()).as("The chart ends before the column beside it, where the query starts")
                .isLessThan(leftOf(pdf, "Query:"));
        assertThat((double) chart.width() / chart.height()).as("The chart keeps its own shape").isCloseTo(CHART_RATIO, within(0.02d));
        assertFalse(compareContentUsingReferenceImages(getCurrentMethodName(), pdf), "The pages differ from the reference images");
    }

    private byte @NotNull [] exportLiveReport() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("Reports/Items By Status")
                .documentType(DocumentType.LIVE_REPORT)
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .fitToPage(true)
                .build();

        DocumentData<IRichPage> report = DocumentData.creator(DocumentType.LIVE_REPORT, mock(IRichPage.class))
                .id(new LiveReportId(new DocumentProject("testProjectId", "Test Project"), "Items By Status"))
                .title("Items by Status")
                .content(readHtmlResource("liveReportPieChart"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(report);

        return converter.convertToPdf(params, null);
    }

    /** Where the first line which starts with the given words starts, in points from the left edge of the page. */
    @SneakyThrows
    private static float leftOf(byte @NotNull [] pdf, @NotNull String words) {
        List<Float> starts = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    if (starts.isEmpty() && text.startsWith(words)) {
                        starts.add(positions.getFirst().getXDirAdj());
                    }
                }
            };
            stripper.getText(document);
        }
        assertThat(starts).as("The report shows '%s'", words).isNotEmpty();
        return starts.getFirst();
    }
}
