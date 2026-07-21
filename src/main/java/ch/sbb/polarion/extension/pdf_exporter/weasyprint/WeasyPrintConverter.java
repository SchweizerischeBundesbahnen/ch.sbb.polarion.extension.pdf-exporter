package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import com.polarion.alm.projects.model.IUniqueObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface WeasyPrintConverter {
    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions);

    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions, DocumentData<? extends IUniqueObject> documentData);

    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions, DocumentData<? extends IUniqueObject> documentData, @Nullable PdfGenerationLog generationLog);

    WeasyPrintInfo getWeasyPrintInfo();

    byte[] convertMergedToPdf(@NotNull List<MergeDocumentData> documents, @NotNull MergeJobStartParams params);

    record MergeDocumentData(@NotNull String htmlContent, @Nullable String coverPageHtml) {}
}
