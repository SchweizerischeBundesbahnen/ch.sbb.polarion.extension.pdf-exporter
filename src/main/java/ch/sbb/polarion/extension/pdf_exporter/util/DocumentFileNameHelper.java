package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.tracker.model.ITrackerProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;


public class DocumentFileNameHelper {
    private final PdfExporterPolarionService pdfExporterPolarionService;

    private final VelocityEvaluator velocityEvaluator;

    public DocumentFileNameHelper(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.velocityEvaluator = new VelocityEvaluator();
    }

    public DocumentFileNameHelper(PdfExporterPolarionService pdfExporterPolarionService, VelocityEvaluator velocityEvaluator) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.velocityEvaluator = velocityEvaluator;
    }

    public String getDocumentFileName(@NotNull ExportParams exportParams) {
        ITrackerProject project = pdfExporterPolarionService.getTrackerProject(exportParams.getProjectId());

        DocumentDataHelper documentDataHelper = new DocumentDataHelper(pdfExporterPolarionService);
        final DocumentData documentData =
                switch (exportParams.getDocumentType()) {
                    case DOCUMENT -> documentDataHelper.getLiveDocument(project, exportParams, false);
                    case REPORT -> documentDataHelper.getLiveReport(project, exportParams, false);
                    case TESTRUN -> documentDataHelper.getTestRun(project, exportParams, false);
                    case WIKI -> documentDataHelper.getWikiDocument(project, exportParams, false);
                };

        FileNameTemplateModel fileNameTemplateModel = getFileNameTemplateModel(ScopeUtils.getScopeFromProject(exportParams.getProjectId()));
        String fileNameTemplate = getFileNameTemplate(exportParams.getDocumentType(), fileNameTemplateModel);
        fileNameTemplate = new PlaceholderProcessor().replacePlaceholders(documentData, exportParams, fileNameTemplate);
        String evaluatedFileName = evaluateVelocity(documentData, fileNameTemplate);
        return replaceIllegalFileNameSymbols(evaluatedFileName);
    }

    @VisibleForTesting
    String evaluateVelocity(DocumentData documentData, String fileNameTemplate) {
        String evaluatedName = velocityEvaluator.evaluateVelocityExpressions(documentData, fileNameTemplate);
        return String.format("%s.pdf", evaluatedName);
    }

    @VisibleForTesting
    String replaceIllegalFileNameSymbols(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private @NotNull String getFileNameTemplate(@NotNull DocumentType documentType, @NotNull FileNameTemplateModel fileNameTemplateModel) {
        return switch (documentType) {
            case DOCUMENT:
                yield fileNameTemplateModel.getDocumentNameTemplate();
            case REPORT:
                yield fileNameTemplateModel.getReportNameTemplate();
            case TESTRUN:
                yield fileNameTemplateModel.getTestrunNameTemplate();
            case WIKI:
                yield fileNameTemplateModel.getWikiNameTemplate();
        };
    }

    private FileNameTemplateModel getFileNameTemplateModel(String scope) {
        try {
            FileNameTemplateSettings fileNameTemplateSettings = (FileNameTemplateSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(FileNameTemplateSettings.FEATURE_NAME);
            return fileNameTemplateSettings.read(scope, SettingId.fromName(NamedSettings.DEFAULT_NAME), null);
        } catch (IllegalStateException e) {
            return ExceptionHandler.handleTransactionIllegalStateException(e, new FileNameTemplateSettings().defaultValues());
        }
    }
}
