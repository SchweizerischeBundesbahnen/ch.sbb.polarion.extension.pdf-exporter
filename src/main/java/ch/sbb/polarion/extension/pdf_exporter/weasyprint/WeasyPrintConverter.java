package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeSessionStartParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import com.polarion.alm.projects.model.IUniqueObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WeasyPrintConverter {
    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions);

    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions, DocumentData<? extends IUniqueObject> documentData);

    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions, DocumentData<? extends IUniqueObject> documentData, @Nullable PdfGenerationLog generationLog);

    WeasyPrintInfo getWeasyPrintInfo();

    @NotNull String startMergeSession(@NotNull MergeSessionStartParams params);

    void addDocumentToSession(@NotNull String sessionId, @NotNull String htmlContent);

    byte[] finishMergeSession(@NotNull String sessionId);
}
