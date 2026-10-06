package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.util.VeraPdfValidationUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.results.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each PDF variant a merge can be asked for, validated with veraPDF as
 * {@link ch.sbb.polarion.extension.pdf_exporter.weasyprint.PdfVariantValidationTest} validates a single export. For
 * PDF/A-4f, which requires embedded files, each document embeds one, and the merge keeps both.
 */
class PdfVariantMergeTest extends BaseBulkProcessingTest {

    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(PdfVariant.class)
    @SneakyThrows
    void mergesIntoAPdfOfTheRequestedVariant(PdfVariant pdfVariant, @TempDir Path tempDir) {
        String html = readHtmlResource("pdfVariantValidation");
        String content = html.substring(html.indexOf("<body>") + "<body>".length(), html.indexOf("</body>"));
        boolean embedsFiles = pdfVariant == PdfVariant.PDF_A_4F;
        ExportParams alpha = liveDoc("Alpha", content, embedsFiles ? List.of(Files.writeString(tempDir.resolve("alpha.txt"), "Alpha")) : null);
        ExportParams bravo = liveDoc("Bravo", content, embedsFiles ? List.of(Files.writeString(tempDir.resolve("bravo.txt"), "Bravo")) : null);
        alpha.setPdfVariant(pdfVariant);
        bravo.setPdfVariant(pdfVariant);

        MergeResult result = converter.convertMergedToPdf(List.of(alpha, bravo));

        writeReportPdf("bulkProcessingMergeAsVariant", pdfVariant.name(), result.pdfBytes());
        ValidationResult validation = VeraPdfValidationUtils.validatePdf(result.pdfBytes(), VeraPdfValidationUtils.mapPdfVariantToVeraPDFFlavour(pdfVariant));
        assertTrue(validation.isCompliant(), "The merged PDF must be compliant with " + pdfVariant + ". Failed rules: " + validation.getTestAssertions());
        if (embedsFiles) {
            assertEquals(Set.of("alpha.txt", "bravo.txt"), embeddedFiles(result.pdfBytes()), "The merge keeps the files of both documents");
        }
    }

    @SneakyThrows
    private static Set<String> embeddedFiles(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDDocumentNameDictionary names = document.getDocumentCatalog().getNames();
            assertNotNull(names, "The merge has no name dictionary");
            return names.getEmbeddedFiles().getNames().keySet();
        }
    }
}
