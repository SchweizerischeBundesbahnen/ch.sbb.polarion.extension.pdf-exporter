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
    public byte[] generatePdfWithTitle(DocumentData<? extends IUniqueObject> documentData, ExportParams exportParams,
                                       String contentHtml, PdfGenerationLog generationLog) {
        WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder()
                .followHTMLPresentationalHints(exportParams.isFollowHTMLPresentationalHints())
                .pdfVariant(exportParams.getPdfVariant())
                .build();

        generationLog.log("Starting generation for document content...");
        byte[] pdfContent = weasyPrintServiceConnector.convertToPdf(contentHtml, weasyPrintOptions, documentData);
        generationLog.log("Document content has been completed");

        generationLog.log("Starting generation for cover page ...");
        long numberOfPages = MediaUtils.getNumberOfPages(pdfContent);
        PlaceholderValues overridenPlaceholderValues = PlaceholderValues.builder()
                .pageNumber("1")
                .pagesTotalCount(String.valueOf(numberOfPages))
                .build();
        String titleHtml = composeTitleHtml(documentData, exportParams, overridenPlaceholderValues);
        byte[] pdfCoverPage = weasyPrintServiceConnector.convertToPdf(titleHtml, weasyPrintOptions);
        generationLog.log("Cover page generation has been completed");

        generationLog.log("Both generations are completed, starting pages merge...");
        byte[] resultBytes = MediaUtils.overwriteFirstPageWithTitle(pdfContent, pdfCoverPage);
        generationLog.log("Pages merge done");
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
