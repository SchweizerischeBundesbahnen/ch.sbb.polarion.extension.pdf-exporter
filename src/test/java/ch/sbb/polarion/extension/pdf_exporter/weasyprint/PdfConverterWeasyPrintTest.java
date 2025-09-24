package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.WikiPageId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWikiPage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PdfConverterWeasyPrintTest extends BasePdfConverterTest {


    @Override
    protected void prepareSpecificMocks() {
        // No specific mocks needed for general tests
    }

    @Override
    protected void setupHeaderFooterSettings() {
        // Test "testFieldKey" custom field & special "PAGE_NUMBER" placeholder substitution in the header
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("HL")
                .headerCenter("HC  {{ testFieldKey }}")
                .headerRight("HR")
                .footerLeft("FL")
                .footerCenter("FC")
                .footerRight("FR_{{ PAGE_NUMBER }}")
                .build());
    }

    @Override
    protected void setupCssSettings() {
        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        when(cssSettings.defaultValues()).thenCallRealMethod();
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(basicCss)
                .build());
    }

    @Override
    protected void setupCoverPageSettings() {
        String basicCss = readCssResource(CSS_BASIC, FONT_REGULAR);
        // Check "testFieldKey" custom field substitution in the title
        // Additionally check PAGE_NUMBER and PAGES_TOTAL_COUNT supported on cover page
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<div>TITLE {{ testFieldKey }} </div> <div>PAGE_NUMBER = {{ PAGE_NUMBER }} and PAGES_TOTAL_COUNT = {{ PAGES_TOTAL_COUNT }}</div>")
                .templateCss(basicCss)
                .build());
        lenient().when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testConverterSimple() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc1 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .content("<div>TEST</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc1);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    @SneakyThrows
    void testConverterComplexWithTitle() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .coverPage("test")
                .build();

        Path tempFile = Files.createTempFile("attachment", ".pdf");
        Files.writeString(tempFile, "test content");

        DocumentData<IModule> liveDoc2 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .content("<div>TEST page 1</div><!--PAGE_BREAK--><!--PORTRAIT_ABOVE--><div>TEST page 2</div><!--PAGE_BREAK--><!--LANDSCAPE_ABOVE--><div>TEST page 3</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .attachmentFiles(List.of(tempFile))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc2);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterSpecialSymbols() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc3 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("specialSymbolsTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("specialSymbols"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc3);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterImageWidthsInColumns() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .fitToPage(true)
                .build();

        DocumentData<IModule> liveDoc3 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("specialSymbolsTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("imageWidthBasedOnColumnsCount"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc3);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterSvgImage() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc4 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("svgImageTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("svgImage"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc4);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterSvgImageAsBase64() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        DocumentData<IModule> liveDoc4 = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("svgImageTitle")
                .lastRevision("12345")
                .revisionPlaceholder("12345")
                .content(readHtmlResource("svgImageAsBase64"))
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc4);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testConverterWiki() {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();

        //test wiki page export + {{ REVISION }} placeholder usage
        DocumentData<IWikiPage> wikiPage = DocumentData.creator(DocumentType.WIKI_PAGE, mock(IWikiPage.class))
                .id(new WikiPageId(new DocumentProject("testProjectId", "Test Project"), "wikiPageId"))
                .title("wikiPage")
                .content("<div>TEST</div>")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(wikiPage);
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder().useCustomValues(true).headerLeft("HL").headerCenter("HC  {{ REVISION }}").headerRight("HR").footerLeft("FL").footerCenter("FC").footerRight("FR_{{ PAGE_NUMBER }}").build());
        params.setDocumentType(DocumentType.WIKI_PAGE);
        params.setLocationPath("wikiFolder/wikiPage");

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }
}
