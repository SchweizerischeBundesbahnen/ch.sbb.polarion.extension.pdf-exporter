package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

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
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.CustomResourceUrlResolver;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.ResourceUrlPolicy;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.ApiKeyProvider;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.testcontainers.containers.Container;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Merges documents through the real bulk processing service, which renders them with WeasyPrint (see
 * {@link SharedBulkProcessingContainers}). The connector is otherwise tested against mocked responses only, so a change
 * of the connector, of the HTML the exporter sends or of the API of the service would break the merge unnoticed (#1112).
 * <p>
 * The documents are exported with the default style package, as the documents of one merge share one style package:
 * every one of them has the cover page, the header and the footer of the export. Their resources are read under the
 * default policy of the extension, which refuses internal addresses. A merge is compared with reference images, which pin
 * what it looks like, and each of its pages says what it shows.
 * <p>
 * The service runs without an API key, its default. Sending a key, and refusing to send it over plain http, is covered
 * by the unit tests of the connector.
 */
// Lenient: a test of the connector alone leaves the stubs of the converter unused
@ExtendWith({CurrentContextExtension.class, TransactionalExecutorExtension.class})
@CurrentContextConfig("pdf-exporter")
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class BaseBulkProcessingTest extends BasePdfConverterTest {

    protected static final String PAGE_BREAK = "<!--PAGE_BREAK--><!--PORTRAIT_ABOVE-->";
    protected static final String LANDSCAPE_PAGE_BREAK = "<!--PAGE_BREAK--><!--LANDSCAPE_ABOVE-->";
    private static final String JOB_STORAGE_DIR = "/data/jobs";

    /** The converter of the base, merging through the service in the container. */
    @Override
    protected void setupConverter() {
        CoverPageProcessor coverPageProcessor = new CoverPageProcessor(placeholderProcessor, velocityEvaluator, getWeasyPrintServiceConnector(),
                coverPageSettings, new PdfTemplateProcessor(), htmlProcessor);
        converter = new PdfConverter(pdfExporterPolarionService, headerFooterSettings, cssSettings, placeholderProcessor, velocityEvaluator,
                coverPageProcessor, getWeasyPrintServiceConnector(), htmlProcessor, new PdfTemplateProcessor(), connector());
    }

    /** A header naming the document and a footer counting its pages, so that each page tells which document it belongs to. */
    @Override
    protected void setupHeaderFooterSettings() {
        when(headerFooterSettings.load(any(), any())).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("{{ DOCUMENT_TITLE }}")
                .headerCenter("")
                .headerRight("Merged through the bulk processing service")
                .footerLeft("")
                .footerCenter("")
                .footerRight("Page {{ PAGE_NUMBER }} of {{ PAGES_TOTAL_COUNT }}")
                .build());
    }

    /** A cover page which names its document and says what it shows, to tell the covers of a merge apart. */
    @Override
    protected void setupCoverPageSettings() {
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<h1>{{ DOCUMENT_TITLE }}</h1>"
                        + "<p>The cover page of this document, page {{ PAGE_NUMBER }} of {{ PAGES_TOTAL_COUNT }}.</p>"
                        + "<p>It takes the place of the placeholder page the exporter sends first, and counts the pages of its own document only, not those of the whole merge.</p>")
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

    protected static @NotNull BulkProcessingServiceConnector connector() {
        return connector(SharedBulkProcessingContainers.bulkProcessingUrl());
    }

    protected static @NotNull BulkProcessingServiceConnector connector(@NotNull String bulkProcessingUrl) {
        return new BulkProcessingServiceConnector(bulkProcessingUrl, SharedBulkProcessingContainers.weasyPrintUrl(),
                new ApiKeyProvider(() -> null, "bulk processing service"));
    }

    /** The export parameters of a document, from the default style package, its content served as that of a LiveDoc. */
    protected @NotNull ExportParams liveDoc(@NotNull String title, @NotNull String content) {
        return liveDoc(title, content, null);
    }

    /** The export parameters of a document which embeds the files, as PDF/A-4f requires. */
    protected @NotNull ExportParams liveDoc(@NotNull String title, @NotNull String content, @Nullable List<Path> attachmentFiles) {
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
                .attachmentFiles(attachmentFiles)
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(document);
        return params;
    }

    /** The pages of a document, one for each text, each headed with the title of the document and saying what it shows. */
    protected static @NotNull String pages(@NotNull String title, @NotNull String... texts) {
        List<String> pages = new ArrayList<>();
        for (String text : texts) {
            pages.add(page(title, text));
        }
        return String.join(PAGE_BREAK, pages);
    }

    protected static @NotNull String page(@NotNull String title, @NotNull String text) {
        return "<h2>" + title + "</h2><p>" + text + "</p>";
    }

    protected static @NotNull MergeJobStartParams startParams() {
        return MergeJobStartParams.builder().pdfVariant(PdfVariant.PDF_A_2B.toWeasyPrintParameter()).build();
    }

    /** A document sent to the connector as the exporter sends it, without a cover page. */
    protected static @NotNull MergeDocumentData rendered(@NotNull String title, @NotNull String text) {
        return new MergeDocumentData(html(page(title, text)), null, DocumentConversionParams.builder().pdfVariant(PdfVariant.PDF_A_2B.toWeasyPrintParameter()).build());
    }

    /** A document WeasyPrint refuses, being asked for a PDF variant it does not know. */
    protected static @NotNull MergeDocumentData failing() {
        return new MergeDocumentData(html(page("Failing", "This document asks for a PDF variant WeasyPrint does not know.")), null,
                DocumentConversionParams.builder().pdfVariant("pdf/x-unknown").build());
    }

    private static @NotNull String html(@NotNull String body) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Merge</title><style>" + readFontCss() + "</style></head><body>" + body + "</body></html>";
    }

    protected void assertMatchesReferenceImages(@NotNull String name, @NotNull MergeResult result) {
        assertFalse(compareContentUsingReferenceImages(name, result.pdfBytes()), "The pages differ from the reference images");
    }

    protected static void assertPage(@NotNull List<String> pages, int index, @NotNull String... texts) {
        for (String text : texts) {
            assertTrue(pages.get(index).contains(text), "Page " + (index + 1) + " holds '" + text + "', but reads: " + pages.get(index));
        }
    }

    @SneakyThrows
    protected static @NotNull List<String> pageTexts(byte[] pdf) {
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
    protected static @NotNull List<PDRectangle> pageSizes(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<PDRectangle> sizes = new ArrayList<>();
            document.getPages().forEach(page -> sizes.add(page.getMediaBox()));
            return sizes;
        }
    }

    /**
     * The jobs the service keeps in its storage, which a failed merge must not add to. A storage which cannot be listed
     * fails, as counting it as empty would let a check of a deleted job pass without checking anything. The storage of
     * a service started elsewhere cannot be listed, so a test which counts the jobs is skipped there.
     */
    @SneakyThrows
    protected static int storedJobs() {
        assumeTrue(SharedBulkProcessingContainers.startedByTheTests(), "The job storage of a bulk processing service started elsewhere cannot be listed");
        Container.ExecResult listing = SharedBulkProcessingContainers.bulkProcessing().execInContainer("ls", "-1", JOB_STORAGE_DIR);
        assertEquals(0, listing.getExitCode(), "Cannot list the job storage of the service: " + listing.getStderr());
        String jobs = listing.getStdout().trim();
        return jobs.isEmpty() ? 0 : jobs.split("\n").length;
    }
}
