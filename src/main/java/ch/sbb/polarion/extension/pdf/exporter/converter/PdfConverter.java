package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.ExportMetaInfoCallback;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.WorkItemRefData;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.EnumValuesProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.LiveDocHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfExporterListStyleProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConnectorFactory;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@SuppressWarnings("java:S1200")
public class PdfConverter {
    private final Logger logger = Logger.getLogger(PdfConverter.class);
    private final PdfExporterPolarionService pdfExporterPolarionService;

    private final HeaderFooterSettings headerFooterSettings;
    private final CssSettings cssSettings;

    private final LiveDocHelper liveDocHelper;
    private final PlaceholderProcessor placeholderProcessor;
    private final VelocityEvaluator velocityEvaluator;
    private final CoverPageProcessor coverPageProcessor;
    private final WeasyPrintConverter weasyPrintConverter;
    private final HtmlProcessor htmlProcessor;
    private final PdfTemplateProcessor pdfTemplateProcessor;

    public PdfConverter() {
        pdfExporterPolarionService = new PdfExporterPolarionService();
        headerFooterSettings = new HeaderFooterSettings();
        cssSettings = new CssSettings();
        liveDocHelper = new LiveDocHelper(pdfExporterPolarionService);
        placeholderProcessor = new PlaceholderProcessor();
        velocityEvaluator = new VelocityEvaluator();
        coverPageProcessor = new CoverPageProcessor();
        weasyPrintConverter = WeasyPrintConnectorFactory.getWeasyPrintExecutor();
        htmlProcessor = new HtmlProcessor(new PdfExporterFileResourceProvider(), new LocalizationSettings());
        pdfTemplateProcessor = new PdfTemplateProcessor();
    }

    public byte[] convertToPdf(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        long startTime = System.currentTimeMillis();

        PdfGenerationLog generationLog = new PdfGenerationLog();
        generationLog.log("Starting html generation");

        ITrackerProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getTrackerProject(exportParams.getProjectId());
        }

        final LiveDocHelper.DocumentData documentData =
                switch (exportParams.getDocumentType()) {
                    case WIKI -> liveDocHelper.getWikiDocument(project, exportParams);
                    case REPORT -> liveDocHelper.getLiveReport(project, exportParams);
                    case DOCUMENT -> liveDocHelper.getLiveDocument(Objects.requireNonNull(project), exportParams, true);
                };

        String cssContent = getCssContent(documentData, exportParams);
        String preparedDocumentContent = postProcessDocumentContent(exportParams, project, documentData.getDocumentContent());
        String headerFooterContent = getHeaderFooterContent(documentData, exportParams);
        HtmlData htmlData = new HtmlData(cssContent, preparedDocumentContent, headerFooterContent);
        String htmlContent = composeHtml(documentData.getDocumentTitle(), htmlData, exportParams);
        if (metaInfoCallback != null) {
            metaInfoCallback.setLinkedWorkItems(WorkItemRefData.extractListFromHtml(htmlContent, exportParams.getProjectId()));
        }

        generationLog.log("Html is ready, starting pdf generation");
        byte[] bytes = generatePdf(documentData, exportParams, metaInfoCallback, htmlContent, generationLog);

        if (exportParams.getInternalContent() == null) { //do not log time for internal parts processing
            String finalMessage = "PDF document '" + documentData.getDocumentTitle() + "' has been generated within " + (System.currentTimeMillis() - startTime) + " milliseconds";
            logger.info(finalMessage);
            generationLog.log(finalMessage);
            if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
                new HtmlLogger().log(documentData.getDocumentContent(), htmlContent, generationLog.getLog());
            }
        }
        return bytes;
    }

    @VisibleForTesting
    byte[] generatePdf(
            LiveDocHelper.DocumentData documentData,
            ExportParams exportParams,
            ExportMetaInfoCallback metaInfoCallback,
            String htmlPage,
            PdfGenerationLog generationLog) {
        if (metaInfoCallback == null && exportParams.getInternalContent() == null && exportParams.getCoverPage() != null) {
            return coverPageProcessor.generatePdfWithTitle(documentData, exportParams, htmlPage, generationLog);
        } else {
            WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder().followHTMLPresentationalHints(exportParams.isFollowHTMLPresentationalHints()).build();
            return weasyPrintConverter.convertToPdf(htmlPage, weasyPrintOptions);
        }
    }

    @VisibleForTesting
    String postProcessDocumentContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @Nullable String documentContent) {
        if (documentContent != null) {
            List<String> selectedRoleEnumValues = project == null ? Collections.emptyList() : EnumValuesProvider.getBidirectionalLinkRoleNames(project, exportParams.getLinkedWorkitemRoles());
            return htmlProcessor.processHtmlForPDF(documentContent, exportParams, selectedRoleEnumValues);
        } else {
            return "";
        }
    }

    @NotNull
    @VisibleForTesting
    String composeHtml(@NotNull String documentName,
                               @NotNull HtmlData htmlData,
                               @NotNull ExportParams exportParams) {
        String content = htmlData.headerFooterContent
                + (exportParams.getCoverPage() != null ? "<div style='break-after:page'>page to be removed</div>" : "") //add fake page which later will be replaced with title
                + "<div class='content'>" + htmlData.documentContent + "</div>";
        return pdfTemplateProcessor.processUsing(exportParams, documentName, htmlData.cssContent, content);
    }

    @NotNull
    @VisibleForTesting
    String getCssContent(
            @NotNull LiveDocHelper.DocumentData documentData,
            @NotNull ExportParams exportParams) {
        String cssSettingsName = exportParams.getCss() != null ? exportParams.getCss() : NamedSettings.DEFAULT_NAME;
        String pdfStyles = cssSettings.load(exportParams.getProjectId(), SettingId.fromName(cssSettingsName)).getCss();
        String listStyles = new PdfExporterListStyleProvider(exportParams.getNumberedListStyles()).getStyle();
        String css = pdfStyles
                + (exportParams.getHeadersColor() != null ?
                  "      h1, h2, h3, h4, h5, h6, .content .title {"
                + "        color: " + exportParams.getHeadersColor() + ";"
                + "      }"
                : "")
                + listStyles;

        String content = placeholderProcessor.replacePlaceholders(documentData, exportParams, css);
        String processed = velocityEvaluator.evaluateVelocityExpressions(documentData, content);

        return (exportParams.getDocumentType() == DocumentType.WIKI) ? appendWikiCss(processed) : processed;
    }

    @VisibleForTesting
    String getHeaderFooterContent(
            @NotNull LiveDocHelper.DocumentData documentData,
            @NotNull ExportParams exportParams) {
        String headerFooterSettingsName = exportParams.getHeaderFooter() != null ? exportParams.getHeaderFooter() : NamedSettings.DEFAULT_NAME;
        HeaderFooterModel headerFooter = headerFooterSettings.load(exportParams.getProjectId(), SettingId.fromName(headerFooterSettingsName));

        List<String> headersFooters = Arrays.asList(
                headerFooter.getHeaderLeft(),
                headerFooter.getHeaderCenter(),
                headerFooter.getHeaderRight(),
                headerFooter.getFooterLeft(),
                headerFooter.getFooterCenter(),
                headerFooter.getFooterRight());

        List<String> headerFooterContents = placeholderProcessor.replacePlaceholders(documentData, exportParams, headersFooters);

        List<String> nonNullHeaderFooterContents = headerFooterContents.stream()
                .map(c -> (c == null) ? "" : c)
                .map(c -> velocityEvaluator.evaluateVelocityExpressions(documentData, c))
                .toList();

        return String.format(ScopeUtils.getFileContent("webapp/pdf-exporter/html/headerAndFooter.html"),
                nonNullHeaderFooterContents.toArray());
    }

    private String appendWikiCss(String css) {
        return css + System.lineSeparator() + ScopeUtils.getFileContent("default/wiki.css");
    }

    record HtmlData(String cssContent, String documentContent, String headerFooterContent) {}
}
