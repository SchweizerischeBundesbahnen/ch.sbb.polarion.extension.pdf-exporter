package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.filename.FileNameTemplateModel;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.velocity.VelocityEvaluator;
import com.polarion.alm.tracker.model.ITrackerProject;
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

    public String getDocumentFileName(String locationPath, String revision, DocumentType documentType, String scope) {
        ITrackerProject project = pdfExporterPolarionService.getProjectFromScope(scope);

        ExportParams exportParams = ExportParams.builder()
                .locationPath(locationPath)
                .documentType(documentType)
                .revision(revision)
                .build();

        LiveDocHelper liveDocHelper = new LiveDocHelper(pdfExporterPolarionService);
        final LiveDocHelper.DocumentData documentData =
                switch (exportParams.getDocumentType()) {
                    case WIKI -> liveDocHelper.getWikiDocument(project, exportParams, false);
                    case REPORT -> liveDocHelper.getLiveReport(project, exportParams, false);
                    case DOCUMENT -> liveDocHelper.getLiveDocument(project, exportParams, false);
                };

        FileNameTemplateModel fileNameTemplateModel = getFileNameTemplateModel(scope);
        String fileNameTemplate = getFileNameTemplate(documentType, fileNameTemplateModel);
        fileNameTemplate = new PlaceholderProcessor().replacePlaceholders(documentData, exportParams, fileNameTemplate);
        String evaluatedFileName = evaluateVelocity(documentData, fileNameTemplate);
        return replaceIllegalFileNameSymbols(evaluatedFileName);
    }

    @VisibleForTesting
    String evaluateVelocity(LiveDocHelper.DocumentData documentData, String fileNameTemplate) {
        String evaluatedName = velocityEvaluator.evaluateVelocityExpressions(documentData, fileNameTemplate);
        return String.format("%s.pdf", evaluatedName);
    }

    @VisibleForTesting
    String replaceIllegalFileNameSymbols(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String getFileNameTemplate(DocumentType documentType, FileNameTemplateModel fileNameTemplateModel) {
        return documentType == DocumentType.DOCUMENT ? fileNameTemplateModel.getDocumentNameTemplate() : fileNameTemplateModel.getReportNameTemplate();
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
