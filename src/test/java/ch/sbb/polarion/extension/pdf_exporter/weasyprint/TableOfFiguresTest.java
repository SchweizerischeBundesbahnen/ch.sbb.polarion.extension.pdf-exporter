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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test for Table of Figures (ToF) and Table of Tables (ToT) generation.
 * Tests that the PDF exporter correctly generates ToF/ToT even when Polarion does not
 * provide anchor elements inside caption spans.
 */
class TableOfFiguresTest extends BasePdfConverterTest {

    @Override
    protected void setupHeaderFooterSettings() {
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("")
                .headerCenter("Table of Figures Test")
                .headerRight("")
                .footerLeft("")
                .footerCenter("{{ PAGE_NUMBER }}")
                .footerRight("")
                .build());
    }

    private static Stream<Arguments> provideTableOfFiguresTestCases() {
        return Stream.of(
                Arguments.of("tableOfFigures", "Table of Figures Test"),
                Arguments.of("tableOfTables", "Table of Tables Test"),
                Arguments.of("tableOfFiguresAndTables", "Combined ToF and ToT Test")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideTableOfFiguresTestCases")
    void testTableOfFiguresGeneration(String htmlResource, String title) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title(title)
                .content(readHtmlResource(htmlResource))
                .lastRevision("1")
                .revisionPlaceholder("1")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        boolean hasDiff = compareContentUsingReferenceImages(htmlResource, converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }
}
