package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf.exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.LiveDocHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.WeasyPrintServiceConnector;
import lombok.SneakyThrows;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CompletableFuture;

public class CoverPageProcessor {
    private final PlaceholderProcessor placeholderProcessor;
    private final VelocityEvaluator velocityEvaluator;
    private final WeasyPrintServiceConnector weasyPrintServiceConnector;
    private final CoverPageSettings coverPageSettings;
    private final PdfTemplateProcessor pdfTemplateProcessor;


    public CoverPageProcessor() {
        placeholderProcessor = new PlaceholderProcessor();
        velocityEvaluator = new VelocityEvaluator();
        weasyPrintServiceConnector = new WeasyPrintServiceConnector();
        coverPageSettings = new CoverPageSettings();
        pdfTemplateProcessor = new PdfTemplateProcessor();
    }

    public CoverPageProcessor(PlaceholderProcessor placeholderProcessor,
                              VelocityEvaluator velocityEvaluator,
                              WeasyPrintServiceConnector weasyPrintServiceConnector,
                              CoverPageSettings coverPageSettings,
                              PdfTemplateProcessor pdfTemplateProcessor) {
        this.placeholderProcessor = placeholderProcessor;
        this.velocityEvaluator = velocityEvaluator;
        this.weasyPrintServiceConnector = weasyPrintServiceConnector;
        this.coverPageSettings = coverPageSettings;
        this.pdfTemplateProcessor = pdfTemplateProcessor;
    }

    @SneakyThrows
    public byte[] generatePdfWithTitle(LiveDocHelper.DocumentData documentData, ExportParams exportParams,
                                       String contentHtml, PdfGenerationLog generationLog) {
        String titleHtml = composeTitleHtml(documentData, exportParams);
        generationLog.log("Starting concurrent generation for cover page and content");
        WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder().followHTMLPresentationalHints(exportParams.isFollowHTMLPresentationalHints()).build();
        CompletableFuture<byte[]> generateTitleFuture = CompletableFuture.supplyAsync(() -> weasyPrintServiceConnector.convertToPdf(titleHtml, weasyPrintOptions));
        CompletableFuture<byte[]> generateContentFuture = CompletableFuture.supplyAsync(() -> weasyPrintServiceConnector.convertToPdf(contentHtml, weasyPrintOptions));
        CompletableFuture.allOf(generateTitleFuture, generateContentFuture).join();
        generationLog.log("Both generations are completed, starting pages merge");

        byte[] resultBytes = MediaUtils.overwriteFirstPageWithTitle(generateContentFuture.get(), generateTitleFuture.get());
        generationLog.log("Pages merge done");
        return resultBytes;
    }

    @VisibleForTesting
    String composeTitleHtml(LiveDocHelper.DocumentData documentData, ExportParams exportParams) {
        CoverPageModel settings = coverPageSettings.load(exportParams.getProjectId(), SettingId.fromName(exportParams.getCoverPage()));
        String templateHtml = settings.getTemplateHtml();
        String content = placeholderProcessor.replacePlaceholders(documentData, exportParams, templateHtml);
        String evaluatedContent = velocityEvaluator.evaluateVelocityExpressions(documentData, content);
        return pdfTemplateProcessor.processUsing(exportParams, documentData.getDocumentTitle(), coverPageSettings.processImagePlaceholders(settings.getTemplateCss()), evaluatedContent);
    }

}
