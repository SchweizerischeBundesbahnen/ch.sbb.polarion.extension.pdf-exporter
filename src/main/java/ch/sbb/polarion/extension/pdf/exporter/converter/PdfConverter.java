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
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.hooks.WebhooksModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.EnumValuesProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.LiveDocHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfExporterListStyleProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConnectorFactory;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.AllArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider();
        htmlProcessor = new HtmlProcessor(fileResourceProvider, new LocalizationSettings(), new HtmlLinksHelper(fileResourceProvider), pdfExporterPolarionService);
        pdfTemplateProcessor = new PdfTemplateProcessor();
    }

    public byte[] convertToPdf(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        long startTime = System.currentTimeMillis();

        PdfGenerationLog generationLog = new PdfGenerationLog();
        generationLog.log("Starting html generation");

        @Nullable ITrackerProject project = getTrackerProject(exportParams);
        @NotNull final LiveDocHelper.DocumentData documentData = getDocumentData(exportParams, project);
        @NotNull String htmlContent = prepareHtmlContent(exportParams, project, documentData, metaInfoCallback);

        generationLog.log("Html is ready, starting pdf generation");
        if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
            new HtmlLogger().log(documentData.getDocumentContent(), htmlContent, generationLog.getLog());
        }
        byte[] bytes = generatePdf(documentData, exportParams, metaInfoCallback, htmlContent, generationLog);

        if (exportParams.getInternalContent() == null) { //do not log time for internal parts processing
            String finalMessage = "PDF document '" + documentData.getDocumentTitle() + "' has been generated within " + (System.currentTimeMillis() - startTime) + " milliseconds";
            logger.info(finalMessage);
            generationLog.log(finalMessage);
        }
        return bytes;
    }

    public @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        @Nullable ITrackerProject project = getTrackerProject(exportParams);
        @NotNull final LiveDocHelper.DocumentData documentData = getDocumentData(exportParams, project);
        return prepareHtmlContent(exportParams, project, documentData, metaInfoCallback);
    }

    private @Nullable ITrackerProject getTrackerProject(@NotNull ExportParams exportParams) {
        ITrackerProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getTrackerProject(exportParams.getProjectId());
        }
        return project;
    }

    private @NotNull LiveDocHelper.DocumentData getDocumentData(@NotNull ExportParams exportParams, @Nullable ITrackerProject project) {
        return switch (exportParams.getDocumentType()) {
            case WIKI -> liveDocHelper.getWikiDocument(project, exportParams);
            case REPORT -> liveDocHelper.getLiveReport(project, exportParams);
            case DOCUMENT -> liveDocHelper.getLiveDocument(Objects.requireNonNull(project), exportParams, true);
        };
    }

    private @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @NotNull LiveDocHelper.DocumentData documentData, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        String cssContent = getCssContent(documentData, exportParams);
        String preparedDocumentContent = postProcessDocumentContent(exportParams, project, documentData.getDocumentContent());
        String headerFooterContent = getHeaderFooterContent(documentData, exportParams);
        HtmlData htmlData = new HtmlData(cssContent, preparedDocumentContent, headerFooterContent);
        String htmlContent = composeHtml(documentData.getDocumentTitle(), htmlData, exportParams);
        if (metaInfoCallback != null) {
            metaInfoCallback.setLinkedWorkItems(WorkItemRefData.extractListFromHtml(htmlContent, exportParams.getProjectId()));
        }
        htmlContent = htmlProcessor.internalizeLinks(htmlContent);
        htmlContent = applyWebooks(exportParams, htmlContent);
        return htmlContent;
    }

    private @NotNull String applyWebooks(@NotNull ExportParams exportParams, @NotNull String htmlContent) {
        if (exportParams.getWebhooks() == null) {
            return htmlContent;
        }

        WebhooksModel webhooksModel = new WebhooksSettings().load(exportParams.getProjectId(), SettingId.fromName(exportParams.getWebhooks()));
        String result = htmlContent;
        for (String webhook : webhooksModel.getWebhooks()) {
            result = applyWebhook(webhook, exportParams, result);
        }
        return result;
    }

    private @NotNull String applyWebhook(@NotNull String webhook, @NotNull ExportParams exportParams, @NotNull String htmlContent) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(webhook).register(MultiPartFeature.class);

            FormDataMultiPart multipart = new FormDataMultiPart();
            multipart.bodyPart(new FormDataBodyPart("exportParams", new ObjectMapper().writeValueAsString(exportParams), MediaType.APPLICATION_JSON_TYPE));
            multipart.bodyPart(new FormDataBodyPart("html", htmlContent.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM_TYPE));

            try (Response response = webTarget.request(MediaType.TEXT_PLAIN).post(Entity.entity(multipart, multipart.getMediaType()))) {
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    try (InputStream inputStream = response.readEntity(InputStream.class)) {
                        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } else {
                    logger.error(String.format("Could not get proper response from webhook [%s]: response status %s", webhook, response.getStatus()));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Could not get response from webhook [%s]", webhook), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }

        // In case of errors return initial HTML without modification
        return htmlContent;
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

        String cssContent = (exportParams.getDocumentType() != DocumentType.DOCUMENT) ? appendWikiCss(processed) : processed;
        return htmlProcessor.replaceImagesAsBase64Encoded(cssContent);
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

        String headerFooterContent = String.format(ScopeUtils.getFileContent("webapp/pdf-exporter/html/headerAndFooter.html"), nonNullHeaderFooterContents.toArray());
        return htmlProcessor.replaceImagesAsBase64Encoded(headerFooterContent);
    }

    private String appendWikiCss(String css) {
        return css + System.lineSeparator() + ScopeUtils.getFileContent("default/wiki.css");
    }

    record HtmlData(String cssContent, String documentContent, String headerFooterContent) {}
}
