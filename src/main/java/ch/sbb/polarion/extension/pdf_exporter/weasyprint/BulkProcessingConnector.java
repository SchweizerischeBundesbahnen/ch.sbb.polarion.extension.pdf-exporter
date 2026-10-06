package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Connector to the bulk processing service, which merges several documents into a single PDF.
 * This is a separate backend service from the WeasyPrint service used for single-document conversion.
 */
public interface BulkProcessingConnector {

    MergeResult convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params);

    /**
     * A document of a merge.
     *
     * @param attachmentFiles the files the document embeds, as PDF/A-4f requires, or {@code null} where it embeds none
     */
    record MergeDocumentData(@NotNull String htmlContent, @Nullable String coverPageHtml, @NotNull DocumentConversionParams params, @Nullable List<Path> attachmentFiles) {
        public MergeDocumentData(@NotNull String htmlContent, @Nullable String coverPageHtml, @NotNull DocumentConversionParams params) {
            this(htmlContent, coverPageHtml, params, null);
        }
    }

    // A payload carrier (the merged PDF bytes); it is never compared by value, so the array-aware
    // equals/hashCode/toString java:S6218 asks for would be dead boilerplate here.
    @SuppressWarnings("java:S6218")
    record MergeResult(byte[] pdfBytes, int failedDocumentCount) {}
}
