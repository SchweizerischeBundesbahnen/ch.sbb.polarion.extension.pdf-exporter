package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.ExportMetaInfoCallback;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WorkItemRefData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.AuthType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhookConfig;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhooksModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.EnumValuesProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterListStyleProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.internal.security.UserAccountVault;
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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@SuppressWarnings("java:S1200")
public class PdfConverter {
    private final Logger logger = Logger.getLogger(PdfConverter.class);
    private final PdfExporterPolarionService pdfExporterPolarionService;

    private final HeaderFooterSettings headerFooterSettings;
    private final CssSettings cssSettings;

    private final PlaceholderProcessor placeholderProcessor;
    private final VelocityEvaluator velocityEvaluator;
    private final CoverPageProcessor coverPageProcessor;
    private final WeasyPrintServiceConnector weasyPrintServiceConnector;
    private final HtmlProcessor htmlProcessor;
    private final PdfTemplateProcessor pdfTemplateProcessor;

    public PdfConverter() {
        pdfExporterPolarionService = new PdfExporterPolarionService();
        headerFooterSettings = new HeaderFooterSettings();
        cssSettings = new CssSettings();
        placeholderProcessor = new PlaceholderProcessor();
        velocityEvaluator = new VelocityEvaluator();
        weasyPrintServiceConnector = new WeasyPrintServiceConnector();
        PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider();
        htmlProcessor = new HtmlProcessor(fileResourceProvider, new LocalizationSettings(), new HtmlLinksHelper(fileResourceProvider), pdfExporterPolarionService);
        coverPageProcessor = new CoverPageProcessor(htmlProcessor);
        pdfTemplateProcessor = new PdfTemplateProcessor();
    }

    public byte[] convertToPdf(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        long startTime = System.currentTimeMillis();

        PdfGenerationLog generationLog = new PdfGenerationLog();
        generationLog.log("Starting html generation");

        @Nullable ITrackerProject project = getTrackerProject(exportParams);
        @NotNull final DocumentData<? extends IUniqueObject> documentData = DocumentDataFactory.getDocumentData(exportParams, true);
        @NotNull String htmlContent = prepareHtmlContent(exportParams, project, documentData, metaInfoCallback);

        generationLog.log("Html is ready, starting pdf generation");
        if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
            new HtmlLogger().log(documentData.getContent(), htmlContent, generationLog.getLog());
        }
        byte[] bytes = generatePdf(documentData, exportParams, metaInfoCallback, htmlContent, generationLog);

        if (exportParams.getInternalContent() == null) { //do not log time for internal parts processing
            String finalMessage = "PDF document '" + documentData.getTitle() + "' has been generated within " + (System.currentTimeMillis() - startTime) + " milliseconds";
            logger.info(finalMessage);
            generationLog.log(finalMessage);
        }
        return bytes;
    }

    public @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        @Nullable ITrackerProject project = getTrackerProject(exportParams);
        @NotNull final DocumentData<? extends IUniqueObject> documentData = DocumentDataFactory.getDocumentData(exportParams, true);
        return prepareHtmlContent(exportParams, project, documentData, metaInfoCallback);
    }

    private @Nullable ITrackerProject getTrackerProject(@NotNull ExportParams exportParams) {
        ITrackerProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getTrackerProject(exportParams.getProjectId());
        }
        return project;
    }

    private @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @NotNull DocumentData<? extends IUniqueObject> documentData, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        String cssContent = getCssContent(documentData, exportParams);
        String preparedDocumentContent = postProcessDocumentContent(exportParams, project, documentData.getContent());
        String headerFooterContent = getHeaderFooterContent(documentData, exportParams);
        HtmlData htmlData = new HtmlData(cssContent, preparedDocumentContent, headerFooterContent);
        String htmlContent = composeHtml(documentData.getTitle(), htmlData, exportParams);
        if (metaInfoCallback != null) {
            metaInfoCallback.setLinkedWorkItems(WorkItemRefData.extractListFromHtml(htmlContent, exportParams.getProjectId()));
        }
        htmlContent = htmlProcessor.internalizeLinks(htmlContent);
        htmlContent = applyWebhooks(exportParams, htmlContent);

        // Add a fake page which later will be replaced with cover page. This should be done post-webhooks not to break this approach.
        return (exportParams.getCoverPage() != null ? "<div style='break-after:page'>page to be removed</div>" : "") + htmlContent;
    }

    private @NotNull String applyWebhooks(@NotNull ExportParams exportParams, @NotNull String htmlContent) {
        // Skip webhooks processing among other if this functionality is not enabled by system administrator
        if (!PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled() || exportParams.getWebhooks() == null) {
            return htmlContent;
        }

        WebhooksModel webhooksModel = new WebhooksSettings().load(exportParams.getProjectId(), SettingId.fromName(exportParams.getWebhooks()));
        String result = htmlContent;
        for (WebhookConfig webhookConfig : webhooksModel.getWebhookConfigs()) {
            result = applyWebhook(webhookConfig, exportParams, result);
        }
        return result;
    }

    private @NotNull String applyWebhook(@NotNull WebhookConfig webhookConfig, @NotNull ExportParams exportParams, @NotNull String htmlContent) {
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            WebTarget webTarget = client.target(webhookConfig.getUrl()).register(MultiPartFeature.class);

            try (FormDataMultiPart multipart = new FormDataMultiPart()) {

                multipart.bodyPart(new FormDataBodyPart("exportParams", new ObjectMapper().writeValueAsString(exportParams), MediaType.APPLICATION_JSON_TYPE));
                multipart.bodyPart(new FormDataBodyPart("html", htmlContent.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_OCTET_STREAM_TYPE));

                Invocation.Builder requestBuilder = webTarget.request(MediaType.TEXT_PLAIN);

                addAuthHeader(webhookConfig, requestBuilder);

                try (Response response = requestBuilder.post(Entity.entity(multipart, multipart.getMediaType()))) {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        try (InputStream inputStream = response.readEntity(InputStream.class)) {
                            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } else {
                        logger.error(String.format("Could not get proper response from webhook [%s]: response status %s", webhookConfig.getUrl(), response.getStatus()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Could not get response from webhook [%s]", webhookConfig.getUrl()), e);
        } finally {
            if (client != null) {
                client.close();
            }
        }

        // In case of errors return initial HTML without modification
        return htmlContent;
    }

    private static void addAuthHeader(@NotNull WebhookConfig webhookConfig, @NotNull Invocation.Builder requestBuilder) {
        if (webhookConfig.getAuthType() == null || webhookConfig.getAuthTokenName() == null) {
            return;
        }

        String authInfoFromUserAccountVault = getAuthInfoFromUserAccountVault(webhookConfig.getAuthType(), webhookConfig.getAuthTokenName());
        if (authInfoFromUserAccountVault == null) {
            return;
        }

        requestBuilder.header(HttpHeaders.AUTHORIZATION, webhookConfig.getAuthType().getAuthHeaderPrefix() + " " + authInfoFromUserAccountVault);
    }

    private static @Nullable String getAuthInfoFromUserAccountVault(@NotNull AuthType authType, @NotNull String authTokenName) {
        @NotNull UserAccountVault.Credentials credentials = UserAccountVault.getInstance().getCredentialsForKey(authTokenName);

        return switch (authType) {
            case BASIC_AUTH -> {
                String authInfo = credentials.getUser() + ":" + credentials.getPassword();
                yield Base64.getEncoder().encodeToString(authInfo.getBytes());
            }
            case BEARER_TOKEN -> credentials.getPassword();
        };
    }

    @VisibleForTesting
    byte[] generatePdf(
            DocumentData<? extends IUniqueObject> documentData,
            ExportParams exportParams,
            ExportMetaInfoCallback metaInfoCallback,
            String htmlPage,
            PdfGenerationLog generationLog) {
        if (metaInfoCallback == null && exportParams.getInternalContent() == null && exportParams.getCoverPage() != null) {
            return coverPageProcessor.generatePdfWithTitle(documentData, exportParams, htmlPage, generationLog);
        } else {
            WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder()
                    .followHTMLPresentationalHints(exportParams.isFollowHTMLPresentationalHints())
                    .pdfVariant(exportParams.getPdfVariant())
                    .build();
            return weasyPrintServiceConnector.convertToPdf(htmlPage, weasyPrintOptions);
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
                + "<div class='content'>" + htmlData.documentContent + "</div>";
        return pdfTemplateProcessor.processUsing(exportParams, documentName, htmlData.cssContent, content);
    }

    @NotNull
    @VisibleForTesting
    String getCssContent(
            @NotNull DocumentData<? extends IUniqueObject> documentData,
            @NotNull ExportParams exportParams) {
        String cssSettingsName = exportParams.getCss() != null ? exportParams.getCss() : NamedSettings.DEFAULT_NAME;
        String defaultStyles = cssSettings.defaultValues().getCss();
        CssModel cssModel = cssSettings.load(exportParams.getProjectId(), SettingId.fromName(cssSettingsName));
        String listStyles = new PdfExporterListStyleProvider(exportParams.getNumberedListStyles()).getStyle();
        String css = (cssModel.isDisableDefaultCss() ? "" : defaultStyles)
                + cssModel.getCss()
                + (exportParams.getHeadersColor() != null ?
                "      h1, h2, h3, h4, h5, h6, .content .title {" +
                "        color: " + exportParams.getHeadersColor() + ";" +
                "      }"
                : "")
                + listStyles;

        String content = placeholderProcessor.replacePlaceholders(documentData, exportParams, css);
        String processed = velocityEvaluator.evaluateVelocityExpressions(documentData, content);

        String cssContent = (exportParams.getDocumentType() != DocumentType.LIVE_DOC) ? appendWikiCss(processed) : processed;
        return htmlProcessor.replaceResourcesAsBase64Encoded(cssContent);
    }

    @VisibleForTesting
    String getHeaderFooterContent(
            @NotNull DocumentData<? extends IUniqueObject> documentData,
            @NotNull ExportParams exportParams) {
        String headerFooterSettingsName = exportParams.getHeaderFooter() != null ? exportParams.getHeaderFooter() : NamedSettings.DEFAULT_NAME;
        HeaderFooterModel headerFooter = headerFooterSettings.load(exportParams.getProjectId(), SettingId.fromName(headerFooterSettingsName));
        if (!headerFooter.isUseCustomValues()) {
            headerFooter = headerFooterSettings.defaultValues();
        }

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
        return htmlProcessor.replaceResourcesAsBase64Encoded(headerFooterContent);
    }

    private String appendWikiCss(String css) {
        return css + System.lineSeparator() + ScopeUtils.getFileContent("default/wiki.css");
    }

    record HtmlData(String cssContent, String documentContent, String headerFooterContent) {
    }
}
