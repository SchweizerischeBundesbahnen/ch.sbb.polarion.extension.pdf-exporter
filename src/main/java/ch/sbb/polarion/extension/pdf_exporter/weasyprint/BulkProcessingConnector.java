package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Connector to the bulk processing service, which merges several documents into a single PDF.
 * This is a separate backend service from the WeasyPrint service used for single-document conversion.
 */
public interface BulkProcessingConnector {

    byte[] convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params);

    record MergeDocumentData(@NotNull String htmlContent, @Nullable String coverPageHtml) {}
}
