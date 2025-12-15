package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderValues;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.projects.model.IUniqueObject;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class CoverPageProcessor {
    private final PlaceholderProcessor placeholderProcessor;
    private final VelocityEvaluator velocityEvaluator;
    private final WeasyPrintServiceConnector weasyPrintServiceConnector;
    private final CoverPageSettings coverPageSettings;
    private final PdfTemplateProcessor pdfTemplateProcessor;
    private final HtmlProcessor htmlProcessor;

    public CoverPageProcessor(HtmlProcessor htmlProcessor) {
        placeholderProcessor = new PlaceholderProcessor();
        velocityEvaluator = new VelocityEvaluator();
        weasyPrintServiceConnector = new WeasyPrintServiceConnector();
        coverPageSettings = new CoverPageSettings();
        pdfTemplateProcessor = new PdfTemplateProcessor();
        this.htmlProcessor = htmlProcessor;
    }

    public CoverPageProcessor(PlaceholderProcessor placeholderProcessor,
                              VelocityEvaluator velocityEvaluator,
                              WeasyPrintServiceConnector weasyPrintServiceConnector,
                              CoverPageSettings coverPageSettings,
                              PdfTemplateProcessor pdfTemplateProcessor,
                              HtmlProcessor htmlProcessor) {
        this.placeholderProcessor = placeholderProcessor;
        this.velocityEvaluator = velocityEvaluator;
        this.weasyPrintServiceConnector = weasyPrintServiceConnector;
        this.coverPageSettings = coverPageSettings;
        this.pdfTemplateProcessor = pdfTemplateProcessor;
        this.htmlProcessor = htmlProcessor;
    }

    @SneakyThrows
    public byte[] generatePdfWithTitle(@NotNull DocumentData<? extends IUniqueObject> documentData,
                                       @NotNull ExportParams exportParams,
                                       @NotNull String contentHtml,
                                       @NotNull WeasyPrintOptions weasyPrintOptions,
                                       @NotNull PdfGenerationLog generationLog) {
        generationLog.log("Starting PDF generation with cover page");

        int htmlSize = contentHtml.length();
        byte[] pdfContent = generationLog.timed("Generate main document PDF",
                () -> weasyPrintServiceConnector.convertToPdf(contentHtml, weasyPrintOptions, documentData, generationLog),
                pdf -> String.format("html_size=%d bytes, pdf_size=%d bytes", htmlSize, pdf.length));

        long numberOfPages = generationLog.timed("Count PDF pages",
                () -> MediaUtils.getNumberOfPages(pdfContent),
                pages -> String.format("pages=%d", pages));

        PlaceholderValues overriddenPlaceholderValues = PlaceholderValues.builder()
                .pageNumber("1")
                .pagesTotalCount(String.valueOf(numberOfPages))
                .build();
        String titleHtml = generationLog.timed("Compose cover page HTML",
                () -> composeTitleHtml(documentData, exportParams, overriddenPlaceholderValues));

        int titleHtmlSize = titleHtml.length();
        byte[] pdfCoverPage = generationLog.timed("Generate cover page PDF",
                () -> weasyPrintServiceConnector.convertToPdf(titleHtml, weasyPrintOptions, null, generationLog),
                pdf -> String.format("html_size=%d bytes, pdf_size=%d bytes", titleHtmlSize, pdf.length));

        byte[] resultBytes = generationLog.timed("Merge PDFs (cover page + document)",
                () -> MediaUtils.overwriteFirstPageWithTitle(pdfContent, pdfCoverPage, weasyPrintOptions.getPdfVariant()),
                result -> String.format("result_size=%d bytes", result.length));

        generationLog.log("PDF generation with cover page completed");
        return resultBytes;
    }

    @VisibleForTesting
    String composeTitleHtml(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams, @Nullable PlaceholderValues overridenPlaceholderValues) {
        CoverPageModel settings = coverPageSettings.load(exportParams.getProjectId(), SettingId.fromName(exportParams.getCoverPage()));
        if (!settings.isUseCustomValues()) {
            settings = coverPageSettings.defaultValues();
        }
        String templateHtml = settings.getTemplateHtml();
        String content = placeholderProcessor.replacePlaceholders(documentData, exportParams, templateHtml, overridenPlaceholderValues);
        content = htmlProcessor.replaceResourcesAsBase64Encoded(content);
        String evaluatedContent = velocityEvaluator.evaluateVelocityExpressions(documentData, content);
        String css = coverPageSettings.processImagePlaceholders(settings.getTemplateCss());
        css = htmlProcessor.replaceResourcesAsBase64Encoded(css);
        return pdfTemplateProcessor.processUsing(exportParams, documentData.getTitle(), css, evaluatedContent);
    }

}
