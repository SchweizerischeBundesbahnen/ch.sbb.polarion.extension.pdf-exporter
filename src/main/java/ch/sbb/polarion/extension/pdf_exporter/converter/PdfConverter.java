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
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.DocIdentifier;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.AuthType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhookConfig;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks.WebhooksModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DebugDataStorage;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.util.EnumValuesProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterListStyleProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfGenerationLog;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PolarionTypes;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.internal.security.UserAccountVault;
import lombok.AllArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
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
    public static final String CUSTOM_METADATA_TAG = "CUSTOM_METADATA";
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
        htmlProcessor = new HtmlProcessor(fileResourceProvider, new LocalizationSettings(), new HtmlLinksHelper(fileResourceProvider));
        coverPageProcessor = new CoverPageProcessor(htmlProcessor);
        pdfTemplateProcessor = new PdfTemplateProcessor();
    }

    public byte[] convertToPdf(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        PdfGenerationLog generationLog = new PdfGenerationLog();
        generationLog.log("Starting PDF generation");

        try {
            // Auto-select style package if needed
            if (exportParams.isAutoSelectStylePackage()) {
                generationLog.timed("Auto-select style package", () -> {
                    StylePackageModel mostSuitableStylePackageModel = pdfExporterPolarionService.getMostSuitableStylePackageModel(DocIdentifier.of(exportParams));
                    exportParams.overwriteByStylePackage(mostSuitableStylePackageModel);
                });
            }

            // Get tracker project and document data
            @Nullable ITrackerProject project = getTrackerProject(exportParams);
            @NotNull final DocumentData<? extends IUniqueObject> documentData = DocumentDataFactory.getDocumentData(exportParams, true);

            // Prepare HTML content
            @NotNull String htmlContent = generationLog.timed("Prepare HTML content",
                    () -> prepareHtmlContent(exportParams, project, documentData, metaInfoCallback, generationLog),
                    html -> String.format("html_size=%d bytes", html.length()));

            // Set HTML size metric
            generationLog.setHtmlSize(htmlContent.length());

            generationLog.log("HTML is ready, starting PDF generation");

            // Generate PDF
            byte[] bytes = generatePdf(documentData, exportParams, metaInfoCallback, htmlContent, generationLog);

            // Set PDF metrics
            setPdfMetrics(generationLog, bytes, exportParams);

            // Finalize timing
            generationLog.finish();

            // Log debug information if enabled
            if (exportParams.getInternalContent() == null) { //do not log time for internal parts processing
                String finalMessage = "PDF document '" + documentData.getTitle() + "' has been generated within " + generationLog.getTotalDurationMs() + " milliseconds";
                logger.info(finalMessage);
                generationLog.log(finalMessage);

                if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
                    String timingReport = generationLog.generateTimingReport(documentData.getTitle());
                    // Save to file system
                    new HtmlLogger().log(documentData.getContent(), htmlContent, timingReport);
                    // Also save to storage for REST API access
                    saveDebugDataToStorage(documentData.getContent(), htmlContent, timingReport, documentData.getTitle());
                }
            }

            return bytes;
        } catch (Exception e) {
            generationLog.finish();
            generationLog.log("PDF generation failed: " + e.getMessage());
            if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
                String timingReport = generationLog.generateTimingReport("FAILED");
                new HtmlLogger().log("", "", timingReport);
                saveDebugDataToStorage("", "", timingReport, "FAILED");
            }
            throw e;
        }
    }

    public @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ExportMetaInfoCallback metaInfoCallback) {
        @Nullable ITrackerProject project = getTrackerProject(exportParams);
        @NotNull final DocumentData<? extends IUniqueObject> documentData = DocumentDataFactory.getDocumentData(exportParams, true);
        return prepareHtmlContent(exportParams, project, documentData, metaInfoCallback, null);
    }

    private @Nullable ITrackerProject getTrackerProject(@NotNull ExportParams exportParams) {
        ITrackerProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getTrackerProject(exportParams.getProjectId());
        }
        return project;
    }

    private @NotNull String prepareHtmlContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @NotNull DocumentData<? extends IUniqueObject> documentData, @Nullable ExportMetaInfoCallback metaInfoCallback, @Nullable PdfGenerationLog generationLog) {
        String cssContent = timedIfNotNull(generationLog, "Get CSS content", () -> getCssContent(documentData, exportParams));
        String preparedDocumentContent = postProcessDocumentContent(exportParams, project, documentData.getContent(), generationLog);
        String headerFooterContent = timedIfNotNull(generationLog, "Get header/footer content", () -> getHeaderFooterContent(documentData, exportParams));

        HtmlData htmlData = new HtmlData(cssContent, preparedDocumentContent, headerFooterContent);

        String metaTags = timedIfNotNull(generationLog, "Build meta tags", () -> buildMetaTags(documentData, exportParams));
        String composedHtml = timedIfNotNull(generationLog, "Compose HTML", () -> composeHtml(documentData.getTitle(), htmlData, exportParams, metaTags));

        if (metaInfoCallback != null) {
            metaInfoCallback.setLinkedWorkItems(WorkItemRefData.extractListFromHtml(composedHtml, exportParams.getProjectId()));
        }

        String internalizedHtml = timedIfNotNull(generationLog, "Internalize links", () -> htmlProcessor.internalizeLinks(composedHtml));
        String htmlContent = timedIfNotNull(generationLog, "Apply webhooks", () -> applyWebhooks(exportParams, internalizedHtml));

        // Add a fake page which later will be replaced with cover page. This should be done post-webhooks not to break this approach.
        return (exportParams.getCoverPage() != null ? "<div style='break-after:page'>page to be removed</div>" : "") + htmlContent;
    }

    private <T> T timedIfNotNull(@Nullable PdfGenerationLog generationLog, String stageName, java.util.function.Supplier<T> supplier) {
        if (generationLog != null) {
            return generationLog.timed(stageName, supplier);
        }
        return supplier.get();
    }

    @SuppressWarnings("java:S1166") // Exception intentionally ignored
    private void setPdfMetrics(PdfGenerationLog generationLog, byte[] pdfBytes, ExportParams exportParams) {
        try {
            int pageCount = (int) MediaUtils.getNumberOfPages(pdfBytes);
            String variant = exportParams.getPdfVariant() != null ? exportParams.getPdfVariant().name() : "PDF_A_2B";
            generationLog.setPdfMetrics(pdfBytes.length, pageCount, variant);
        } catch (Exception e) {
            // Ignore errors in metrics collection
            generationLog.setPdfMetrics(pdfBytes.length, 0, null);
        }
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
    static boolean metaTagsPresent(String htmlPage) {
        return htmlPage != null && htmlPage.contains("<!--" + CUSTOM_METADATA_TAG + "-->");
    }

    @VisibleForTesting
    byte[] generatePdf(
            @NotNull DocumentData<? extends IUniqueObject> documentData,
            @NotNull ExportParams exportParams,
            @Nullable ExportMetaInfoCallback metaInfoCallback,
            @NotNull String htmlPage,
            @NotNull PdfGenerationLog generationLog) {

        WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder()
                .followHTMLPresentationalHints(exportParams.isFollowHTMLPresentationalHints())
                .pdfVariant(exportParams.getPdfVariant())
                .customMetadata(metaTagsPresent(htmlPage))
                .imageDensity(exportParams.getImageDensity())
                .fullFonts(exportParams.isFullFonts())
                .build();

        if (metaInfoCallback == null && exportParams.getInternalContent() == null && exportParams.getCoverPage() != null) {
            return coverPageProcessor.generatePdfWithTitle(documentData, exportParams, htmlPage, weasyPrintOptions, generationLog);
        } else {
            return weasyPrintServiceConnector.convertToPdf(htmlPage, weasyPrintOptions, documentData, generationLog);
        }
    }

    @VisibleForTesting
    String postProcessDocumentContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @Nullable String documentContent) {
        return postProcessDocumentContent(exportParams, project, documentContent, null);
    }

    String postProcessDocumentContent(@NotNull ExportParams exportParams, @Nullable ITrackerProject project, @Nullable String documentContent, @Nullable PdfGenerationLog generationLog) {
        if (documentContent != null) {
            List<String> selectedRoleEnumValues = project == null ? Collections.emptyList() : EnumValuesProvider.getBidirectionalLinkRoleNames(project, exportParams.getLinkedWorkitemRoles());
            return htmlProcessor.processHtmlForPDF(documentContent, exportParams, selectedRoleEnumValues, generationLog);
        } else {
            return "";
        }
    }

    @NotNull
    @VisibleForTesting
    String composeHtml(@NotNull String documentName,
                       @NotNull HtmlData htmlData,
                       @NotNull ExportParams exportParams,
                       @NotNull String metaTags) {
        String content = htmlData.headerFooterContent
                + "<div class='content'>" + htmlData.documentContent + "</div>";
        return pdfTemplateProcessor.processUsing(exportParams, documentName, htmlData.cssContent, content, metaTags);
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

    private String buildMetaTags(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams) {
        if (exportParams.getDocumentType() != DocumentType.LIVE_DOC) {
            return "";
        }
        List<String> metadataFields = exportParams.getMetadataFields();
        if (metadataFields == null || metadataFields.isEmpty()) {
            return "";
        }
        if (!(documentData.getDocumentObject() instanceof IModule module)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String metadataField : metadataFields) {
            Object valueObject = module.getValue(metadataField);
            String value = PolarionTypes.convertFieldValueToString(valueObject);
            if (!value.isEmpty()) {
                result
                        .append("<meta name=\"")
                        .append(StringEscapeUtils.escapeHtml4(metadataField))
                        .append("\" content=\"")
                        .append(StringEscapeUtils.escapeHtml4(value))
                        .append("\"/>");
            }
        }
        if (!result.isEmpty()) {
            return "<!--" + CUSTOM_METADATA_TAG + "-->" + result + "<!--/" + CUSTOM_METADATA_TAG + "-->";
        }
        return "";
    }

    private void saveDebugDataToStorage(@Nullable String originalHtml,
                                        @Nullable String processedHtml,
                                        @Nullable String timingReport,
                                        @Nullable String documentTitle) {
        String jobId = DebugDataStorage.getCurrentJobId();
        if (jobId == null) {
            // Not running as async job, skip storage
            return;
        }

        String currentUser = pdfExporterPolarionService.getSecurityService().getCurrentUser();
        if (currentUser == null) {
            currentUser = "unknown";
        }

        DebugDataStorage.storeForCurrentJob(originalHtml, processedHtml, timingReport, currentUser, documentTitle);
    }

    record HtmlData(String cssContent, String documentContent, String headerFooterContent) {
    }
}
