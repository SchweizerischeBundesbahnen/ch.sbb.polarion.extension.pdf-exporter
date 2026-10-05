package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.ApiKeyProvider;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Merges documents through the real bulk processing service, which renders them with WeasyPrint, both in containers on
 * one network. The connector is otherwise tested against mocked responses only, so a change of the connector, of the
 * HTML it sends or of the API of the service would break the merge unnoticed (#1112).
 * <p>
 * The service runs without an API key, its default. Sending a key, and refusing to send it over plain http, is covered
 * by the unit tests of the connector.
 * <p>
 * Besides the text of each page, the merge with a cover page is compared with reference images, as the other
 * integration tests are.
 */
class BulkProcessingServiceTest extends BaseWeasyPrintTest {

    private static final String WEASYPRINT_IMAGE = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";
    private static final String BULK_PROCESSING_IMAGE = "ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest";
    private static final String WEASYPRINT_ALIAS = "weasyprint-service";
    private static final int WEASYPRINT_PORT = 9080;
    private static final int BULK_PROCESSING_PORT = 9070;
    private static final String JOB_STORAGE_DIR = "/data/jobs";
    private static final String PDF_VARIANT = "pdf/a-2b";

    /** What the exporter puts first where a document has a cover page, for the service to replace (see PdfConverter). */
    private static final String COVER_PAGE_PLACEHOLDER = "<div style='break-after:page'>page to be removed</div>";

    private static Network network;
    private static GenericContainer<?> weasyPrint;
    private static GenericContainer<?> bulkProcessing;
    private static BulkProcessingServiceConnector connector;

    @BeforeAll
    @SuppressWarnings("resource") // the containers and the network are closed in tearDown
    static void setUp() {
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

        String bulkProcessingUrl = "http://" + bulkProcessing.getHost() + ":" + bulkProcessing.getMappedPort(BULK_PROCESSING_PORT);
        String weasyPrintUrl = "http://" + weasyPrint.getHost() + ":" + weasyPrint.getMappedPort(WEASYPRINT_PORT);
        connector = new BulkProcessingServiceConnector(bulkProcessingUrl, weasyPrintUrl, new ApiKeyProvider(() -> null, "bulk processing service"));
    }

    @AfterAll
    static void tearDown() {
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

    @Test
    void mergesTheDocumentsInTheOrderTheyAreAdded() {
        MergeResult result = connector.convertMergedToPdf(List.of(
                document(pages("Alpha", 1), null),
                document(pages("Bravo", 2), null),
                document(pages("Charlie", 1), null)), startParams());

        assertEquals(0, result.failedDocumentCount());
        assertEquals(List.of("Alpha 1", "Bravo 1", "Bravo 2", "Charlie 1"), pageTexts(result.pdfBytes()));
    }

    @Test
    void replacesThePlaceholderPageWithTheCoverPage() {
        MergeResult result = connector.convertMergedToPdf(List.of(
                document(COVER_PAGE_PLACEHOLDER + pages("Alpha", 2), html("<p>Cover of Alpha, page {{ PAGE_NUMBER }} of {{ PAGES_TOTAL_COUNT }}</p>")),
                document(pages("Bravo", 1), null)), startParams());

        assertEquals(0, result.failedDocumentCount());
        // The cover takes the place of the placeholder, and counts the pages of its own document, the placeholder included
        assertEquals(List.of("Cover of Alpha, page 1 of 3", "Alpha 1", "Alpha 2", "Bravo 1"), pageTexts(result.pdfBytes()));
        assertFalse(compareContentUsingReferenceImages("bulkProcessingMergeWithCoverPage", result.pdfBytes()), "The pages differ from the reference images");
    }

    @Test
    void countsADocumentWhichFailsToRenderAndMergesTheOthers() {
        MergeResult result = connector.convertMergedToPdf(List.of(
                document(pages("Alpha", 1), null),
                failingDocument(),
                document(pages("Charlie", 1), null)), startParams());

        assertEquals(1, result.failedDocumentCount());
        assertEquals(List.of("Alpha 1", "Charlie 1"), pageTexts(result.pdfBytes()));
    }

    @Test
    @SneakyThrows
    void deletesTheJobOfAMergeWhichFails() {
        int jobsBefore = storedJobs();

        // A merge of documents which all fail to render has nothing to merge, and the service refuses to finish it
        List<MergeDocumentData> documents = List.of(failingDocument(), failingDocument());
        MergeJobStartParams params = startParams();
        assertThrows(IllegalStateException.class, () -> connector.convertMergedToPdf(documents, params));

        assertEquals(jobsBefore, storedJobs(), "The job of the failed merge is deleted, not left for the time to live of the service");
    }

    @Test
    void readsTheVersionOfTheService() {
        assertNotNull(connector.getVersionInfo().getBulkProcessingService());
    }

    private static @NotNull MergeJobStartParams startParams() {
        return MergeJobStartParams.builder().pdfVariant(PDF_VARIANT).build();
    }

    private static @NotNull MergeDocumentData document(@NotNull String body, @Nullable String coverPageHtml) {
        return new MergeDocumentData(html(body), coverPageHtml, DocumentConversionParams.builder().pdfVariant(PDF_VARIANT).build());
    }

    /** A document WeasyPrint refuses, being asked for a PDF variant it does not know. */
    private static @NotNull MergeDocumentData failingDocument() {
        return new MergeDocumentData(html(pages("Failing", 1)), null, DocumentConversionParams.builder().pdfVariant("pdf/x-unknown").build());
    }

    /** Pages each holding only their name and number, as "Bravo 2". */
    private static @NotNull String pages(@NotNull String name, int count) {
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= count; page++) {
            pages.add("<p>" + name + " " + page + "</p>");
        }
        return String.join("<div style='break-after:page'></div>", pages);
    }

    private static @NotNull String html(@NotNull String body) {
        // The font of an export, embedded, so that the pages look the same wherever WeasyPrint runs
        return "<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Merge</title><style>" + readFontCss() + "</style></head><body>" + body + "</body></html>";
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
    private static int storedJobs() {
        String listing = bulkProcessing.execInContainer("ls", "-1", JOB_STORAGE_DIR).getStdout().trim();
        return listing.isEmpty() ? 0 : listing.split("\n").length;
    }
}
