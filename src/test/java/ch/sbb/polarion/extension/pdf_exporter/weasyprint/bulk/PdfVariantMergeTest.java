package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.util.VeraPdfValidationUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeResult;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.results.ValidationResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each PDF variant a merge can be asked for, validated with veraPDF as
 * {@link ch.sbb.polarion.extension.pdf_exporter.weasyprint.PdfVariantValidationTest} validates a single export. PDF/UA-2
 * is left out as there, being incomplete in WeasyPrint, and so is PDF/A-4f, which requires embedded files a merge does
 * not carry (#1166).
 */
class PdfVariantMergeTest extends BaseBulkProcessingTest {

    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = PdfVariant.class, names = {"PDF_UA_2", "PDF_A_4F"}, mode = EnumSource.Mode.EXCLUDE)
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
}
