package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.converter.CoverPageProcessor;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.CustomResourceUrlResolver;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.ResourceUrlPolicy;
import ch.sbb.polarion.extension.pdf_exporter.util.VeraPdfValidationUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.ApiKeyProvider;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import com.polarion.alm.tracker.model.IModule;
import jakarta.ws.rs.ProcessingException;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.results.ValidationResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Merges documents through the real bulk processing service, which renders them with WeasyPrint, both in containers on
 * one network. The connector is otherwise tested against mocked responses only, so a change of the connector, of the
 * HTML the exporter sends or of the API of the service would break the merge unnoticed (#1112).
 * <p>
 * The documents are exported with the default style package, as the documents of one merge share one style package:
 * every one of them has the cover page, the header and the footer of the export. Their resources are read under the
 * default policy of the extension, which refuses internal addresses. Every merge is compared with reference images,
 * which pin what it looks like. The failures go through the connector itself, where a document can be made to fail.
 * <p>
 * The service runs without an API key, its default. Sending a key, and refusing to send it over plain http, is covered
 * by the unit tests of the connector.
 */
// Lenient: the failures go through the connector alone and leave the stubs of the converter unused
@ExtendWith({CurrentContextExtension.class, TransactionalExecutorExtension.class})
@CurrentContextConfig("pdf-exporter")
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkProcessingServiceTest extends BasePdfConverterTest {

    private static final String WEASYPRINT_IMAGE = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";
    private static final String BULK_PROCESSING_IMAGE = "ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest";
    private static final String WEASYPRINT_ALIAS = "weasyprint-service";
    private static final int WEASYPRINT_PORT = 9080;
    private static final int BULK_PROCESSING_PORT = 9070;
    private static final String JOB_STORAGE_DIR = "/data/jobs";
    private static final String PAGE_BREAK = "<!--PAGE_BREAK--><!--PORTRAIT_ABOVE-->";
    private static final String LANDSCAPE_PAGE_BREAK = "<!--PAGE_BREAK--><!--LANDSCAPE_ABOVE-->";

    private static Network network;
    private static GenericContainer<?> weasyPrint;
    private static GenericContainer<?> bulkProcessing;
    private static String bulkProcessingUrl;
    private static String weasyPrintUrl;

    @BeforeAll
    @SuppressWarnings("resource") // the containers and the network are closed in stopContainers
    static void startContainers() {
        network = Network.newNetwork();
        weasyPrint = new GenericContainer<>(WEASYPRINT_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(WEASYPRINT_ALIAS)
                .withExposedPorts(WEASYPRINT_PORT)
                .waitingFor(Wait.forHttp("/version").forPort(WEASYPRINT_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
        weasyPrint.start();
        bulkProcessing = new GenericContainer<>(BULK_PROCESSING_IMAGE)
                .withNetwork(network)
                .withEnv("WEASYPRINT_SERVICE_URL", "http://" + WEASYPRINT_ALIAS + ":" + WEASYPRINT_PORT)
                .withExposedPorts(BULK_PROCESSING_PORT)
                .waitingFor(Wait.forHttp("/ready").forPort(BULK_PROCESSING_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
        bulkProcessing.start();

        bulkProcessingUrl = "http://" + bulkProcessing.getHost() + ":" + bulkProcessing.getMappedPort(BULK_PROCESSING_PORT);
        weasyPrintUrl = "http://" + weasyPrint.getHost() + ":" + weasyPrint.getMappedPort(WEASYPRINT_PORT);
        VeraGreenfieldFoundryProvider.initialise();
    }

    @AfterAll
    static void stopContainers() {
        if (bulkProcessing != null) {
            bulkProcessing.stop();
        }
        if (weasyPrint != null) {
            weasyPrint.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    /** The converter of the base, merging through the service in the container. */
    @Override
    protected void setupConverter() {
        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(placeholderProcessor, velocityEvaluator, getWeasyPrintServiceConnector(),
                coverPageSettings, new PdfTemplateProcessor(), htmlProcessor);
        converter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, placeholderProcessor, velocityEvaluator,
                coverPageProcessor, getWeasyPrintServiceConnector(), htmlProcessor, new PdfTemplateProcessor(), connector());
    }

    /** A cover page naming its document and counting its pages, to tell the covers of a merge apart. */
    @Override
    protected void setupCoverPageSettings() {
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<div>Cover of {{ DOCUMENT_TITLE }}</div><div>Page {{ PAGE_NUMBER }} of {{ PAGES_TOTAL_COUNT }}</div>")
                .templateCss(readFontCss())
                .build());
        lenient().when(coverPageSettings.processImagePlaceholders(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** The resources of a document read as an export reads them, under the default policy, which refuses internal addresses. */
    @Override
    protected void setupHelperComponents() {
        super.setupHelperComponents();
        ResourceUrlPolicy policy = new ResourceUrlPolicy(ResourceUrlPolicy.Mode.BLOCK_INTERNAL, null, null,
                PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_MAX_SIZE_MB_DEFAULT_VALUE);
        fileResourceProvider = new PdfExporterFileResourceProvider(List.of(new CustomResourceUrlResolver(policy)), policy);
        HtmlLinksHelper htmlLinksHelper = mock(HtmlLinksHelper.class);
        when(htmlLinksHelper.internalizeLinks(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        htmlProcessor = new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper);
    }

    // The happy path: documents exported with the default style package

    @Test
    void mergesDocumentsWithTheDefaultStylePackage() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", "<p>Alpha 1</p>" + PAGE_BREAK + "<p>Alpha 2</p>"),
                liveDoc("Bravo", "<p>Bravo 1</p>"),
                liveDoc("Charlie", "<p>Charlie 1</p>" + PAGE_BREAK + "<p>Charlie 2</p>" + PAGE_BREAK + "<p>Charlie 3</p>")));

        assertEquals(0, result.failedDocumentCount());
        List<String> pages = pageTexts(result.pdfBytes());
        assertEquals(9, pages.size(), "Each document is its cover page and its own pages");
        // Each cover takes the place of its placeholder and counts the pages of its own document, the cover included
        assertPage(pages, 0, "Cover of Alpha", "Page 1 of 3");
        assertPage(pages, 1, "Alpha 1");
        assertPage(pages, 2, "Alpha 2");
        assertPage(pages, 3, "Cover of Bravo", "Page 1 of 2");
        assertPage(pages, 4, "Bravo 1");
        assertPage(pages, 5, "Cover of Charlie", "Page 1 of 4");
        assertPage(pages, 6, "Charlie 1");
        assertPage(pages, 8, "Charlie 3");
        assertMatchesReferenceImages("bulkProcessingMergeWithDefaultStylePackage", result);
    }

    /**
     * Each PDF variant a merge can be asked for, validated with veraPDF as {@link PdfVariantValidationTest} validates a
     * single export. PDF/UA-2 is left out as there, being incomplete in WeasyPrint, and so is PDF/A-4f, which requires
     * embedded files a merge does not carry.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = PdfVariant.class, names = {"PDF_UA_2", "PDF_A_4F"}, mode = EnumSource.Mode.EXCLUDE)
    @Disabled("#1161: the merge drops the catalog of each document, so no variant is compliant")
    @SneakyThrows
    void mergesIntoAPdfOfTheRequestedVariant(PdfVariant pdfVariant) {
        String html = readHtmlResource("pdfVariantValidation");
        String content = html.substring(html.indexOf("<body>") + "<body>".length(), html.indexOf("</body>"));
        ExportParams alpha = liveDoc("Alpha", content);
        ExportParams bravo = liveDoc("Bravo", content);
        alpha.setPdfVariant(pdfVariant);
        bravo.setPdfVariant(pdfVariant);

        MergeResult result = converter.convertMergedToPdf(List.of(alpha, bravo));

        writeReportPdf("bulkProcessingMergeAsVariant", pdfVariant.name(), result.pdfBytes());
        ValidationResult validation = VeraPdfValidationUtils.validatePdf(result.pdfBytes(), VeraPdfValidationUtils.mapPdfVariantToVeraPDFFlavour(pdfVariant));
        assertTrue(validation.isCompliant(), "The merged PDF must be compliant with " + pdfVariant + ". Failed rules: " + validation.getTestAssertions());
    }

    // Edge cases

    @Test
    void mergesASingleDocument() {
        MergeResult result = converter.convertMergedToPdf(List.of(liveDoc("Alpha", "<p>Alpha 1</p>")));

        List<String> pages = pageTexts(result.pdfBytes());
        assertEquals(2, pages.size());
        assertPage(pages, 0, "Cover of Alpha", "Page 1 of 2");
        assertPage(pages, 1, "Alpha 1");
        assertMatchesReferenceImages("bulkProcessingMergeOfASingleDocument", result);
    }

    @Test
    void keepsALandscapePageOfADocument() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", "<p>Alpha 1</p>" + LANDSCAPE_PAGE_BREAK + "<p>Alpha 2</p>"),
                liveDoc("Bravo", "<p>Bravo 1</p>")));

        List<PDRectangle> sizes = pageSizes(result.pdfBytes());
        assertEquals(5, sizes.size(), "Each document is its cover page and its own pages");
        assertTrue(sizes.get(1).getWidth() > sizes.get(1).getHeight(), "The page above the landscape break is landscape");
        assertTrue(sizes.get(2).getWidth() < sizes.get(2).getHeight(), "The page after it is portrait again");
        assertTrue(sizes.get(4).getWidth() < sizes.get(4).getHeight(), "The next document is portrait");
        assertMatchesReferenceImages("bulkProcessingMergeWithALandscapePage", result);
    }

    @Test
    void mergesADocumentWithoutContent() {
        MergeResult result = converter.convertMergedToPdf(List.of(liveDoc("Alpha", ""), liveDoc("Bravo", "<p>Bravo 1</p>")));

        assertEquals(0, result.failedDocumentCount());
        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 0, "Cover of Alpha");
        assertPage(pages, pages.size() - 1, "Bravo 1");
        assertMatchesReferenceImages("bulkProcessingMergeWithADocumentWithoutContent", result);
    }

    @Test
    void keepsTextOfAnyScript() {
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Ärger", "<p>Grüße aus Zürich</p>"),
                liveDoc("Требования", "<p>Требования к подвижному составу</p>")));

        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 0, "Cover of Ärger");
        assertPage(pages, 1, "Grüße aus Zürich");
        assertPage(pages, 2, "Cover of Требования");
        assertPage(pages, 3, "Требования к подвижному составу");
        assertMatchesReferenceImages("bulkProcessingMergeOfAnyScript", result);
    }

    @Test
    void mergesADocumentWhoseResourcesThePolicyRefuses() {
        // Internal addresses, which the default policy refuses before any request: loopback, the metadata of a cloud and a private network
        MergeResult result = converter.convertMergedToPdf(List.of(
                liveDoc("Alpha", "<p>Alpha 1</p>"
                        + "<p><img src='http://127.0.0.1:9/logo.png' alt='loopback'/></p>"
                        + "<p><img src='http://169.254.169.254/latest/meta-data/' alt='metadata'/></p>"
                        + "<div style='background-image: url(http://10.0.0.1/background.png); height: 50px;'>Alpha 2</div>"),
                liveDoc("Bravo", "<p>Bravo 1</p>")));

        assertEquals(0, result.failedDocumentCount(), "A refused resource is left out, the document is still exported");
        List<String> pages = pageTexts(result.pdfBytes());
        assertPage(pages, 1, "Alpha 1", "Alpha 2");
        assertPage(pages, 3, "Bravo 1");
        assertMatchesReferenceImages("bulkProcessingMergeWithRefusedResources", result);
    }

    @Test
    void readsTheVersionOfTheService() {
        assertNotNull(connector().getVersionInfo().getBulkProcessingService());
    }

    // Failures, through the connector, where a document can be made to fail

    @Test
    void countsADocumentWhichFailsToRenderAndMergesTheOthers() {
        MergeResult result = connector().convertMergedToPdf(List.of(rendered("Alpha"), failing(), rendered("Charlie")), startParams());

        assertEquals(1, result.failedDocumentCount());
        assertEquals(List.of("Alpha", "Charlie"), pageTexts(result.pdfBytes()));
        assertMatchesReferenceImages("bulkProcessingMergeWithAFailedDocument", result);
    }

    @Test
    void failsAndDeletesTheJobWhereNoDocumentRenders() {
        int jobsBefore = storedJobs();
        BulkProcessingServiceConnector connector = connector();
        List<MergeDocumentData> documents = List.of(failing(), failing());
        MergeJobStartParams params = startParams();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> connector.convertMergedToPdf(documents, params));

        assertTrue(failure.getMessage().contains("All 2 documents failed to convert"), failure.getMessage());
        assertEquals(jobsBefore, storedJobs(), "The job of the failed merge is deleted, not left for the time to live of the service");
    }

    @Test
    void deletesTheJobOfACancelledMerge() {
        int jobsBefore = storedJobs();
        BulkProcessingServiceConnector connector = connector();
        List<MergeDocumentData> documents = List.of(rendered("Alpha"));
        MergeJobStartParams params = startParams();

        Thread.currentThread().interrupt();
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> connector.convertMergedToPdf(documents, params));

        assertTrue(failure.getMessage().contains("was cancelled"), failure.getMessage());
        assertFalse(Thread.currentThread().isInterrupted(), "The flag is cleared, so that a reused worker thread is not cancelled too");
        assertEquals(jobsBefore, storedJobs(), "The job of the cancelled merge is deleted");
    }

    @Test
    void failsWhereTheServiceCannotBeReached() {
        BulkProcessingServiceConnector unreachable = new BulkProcessingServiceConnector("http://localhost:1", weasyPrintUrl, noApiKey());
        List<MergeDocumentData> documents = List.of(rendered("Alpha"));
        MergeJobStartParams params = startParams();

        assertThrows(ProcessingException.class, () -> unreachable.convertMergedToPdf(documents, params));
    }

    private static @NotNull BulkProcessingServiceConnector connector() {
        return new BulkProcessingServiceConnector(bulkProcessingUrl, weasyPrintUrl, noApiKey());
    }

    private static @NotNull ApiKeyProvider noApiKey() {
        return new ApiKeyProvider(() -> null, "bulk processing service");
    }

    /** The export parameters of a document, from the default style package, its content served as that of a LiveDoc. */
    private @NotNull ExportParams liveDoc(@NotNull String title, @NotNull String content) {
        ExportParams params = ExportParams.builder()
                .projectId("testProjectId")
                .locationPath("_default/" + title)
                .documentType(DocumentType.LIVE_DOC)
                .build();
        params.overwriteByStylePackage(new StylePackageSettings(mock(SettingsService.class)).defaultValues());

        DocumentData<IModule> document = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", title))
                .title(title)
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(document);
        return params;
    }

    private static @NotNull MergeJobStartParams startParams() {
        return MergeJobStartParams.builder().pdfVariant(PdfVariant.PDF_A_2B.toWeasyPrintParameter()).build();
    }

    private static @NotNull MergeDocumentData rendered(@NotNull String text) {
        return new MergeDocumentData(html(text), null, DocumentConversionParams.builder().pdfVariant(PdfVariant.PDF_A_2B.toWeasyPrintParameter()).build());
    }

    /** A document WeasyPrint refuses, being asked for a PDF variant it does not know. */
    private static @NotNull MergeDocumentData failing() {
        return new MergeDocumentData(html("Failing"), null, DocumentConversionParams.builder().pdfVariant("pdf/x-unknown").build());
    }

    private static @NotNull String html(@NotNull String text) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Merge</title><style>" + readFontCss() + "</style></head><body><p>" + text + "</p></body></html>";
    }

    private void assertMatchesReferenceImages(@NotNull String name, @NotNull MergeResult result) {
        assertFalse(compareContentUsingReferenceImages(name, result.pdfBytes()), "The pages differ from the reference images");
    }

    private static void assertPage(@NotNull List<String> pages, int index, @NotNull String... texts) {
        for (String text : texts) {
            assertTrue(pages.get(index).contains(text), "Page " + (index + 1) + " holds '" + text + "', but reads: " + pages.get(index));
        }
    }

    @SneakyThrows
    private static @NotNull List<String> pageTexts(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> texts = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                texts.add(stripper.getText(document).trim());
            }
            return texts;
        }
    }

    @SneakyThrows
    private static @NotNull List<PDRectangle> pageSizes(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDRectangle> sizes = new ArrayList<>();
            document.getPages().forEach(page -> sizes.add(page.getMediaBox()));
            return sizes;
        }
    }

    @SneakyThrows
    private static int storedJobs() {
        String listing = bulkProcessing.execInContainer("ls", "-1", JOB_STORAGE_DIR).getStdout().trim();
        return listing.isEmpty() ? 0 : listing.split("\n").length;
    }
}
