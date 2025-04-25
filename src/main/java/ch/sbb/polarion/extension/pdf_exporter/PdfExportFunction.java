package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import com.polarion.alm.tracker.internal.baseline.BaseObjectBaselinesSearch;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.WrapperException;
import com.polarion.portal.internal.shared.navigation.ProjectScope;
import com.polarion.portal.server.PObjectDataProvider;
import com.polarion.subterra.base.data.model.internal.EnumType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PdfExportFunction implements IFunction<IModule> {

    private static final String PARAM_NAME_PROJECT_ID = "project_id";
    private static final String PARAM_NAME_EXISTING_WORK_ITEM_ID = "existing_wi_id";
    private static final String PARAM_NAME_CREATE_WORK_ITEM_TYPE = "create_wi_type_id";
    private static final String PARAM_NAME_CREATE_WORK_ITEM_TITLE = "create_wi_title";
    private static final String PARAM_NAME_CREATE_WORK_ITEM_DESCRIPTION = "create_wi_description";
    private static final String PARAM_NAME_ATTACHMENT_TITLE = "attachment_title";
    private static final String PARAM_NAME_STYLE_PACKAGE = "style_package";
    private static final String PARAM_NAME_PREFER_LAST_BASELINE = "prefer_last_baseline";

    private static final String STYLE_PACKAGE_DEFAULT = "Default";
    private final PdfExporterPolarionService pdfExporterPolarionService;
    private final PdfConverter pdfConverter;

    @SuppressWarnings("unused")
    public PdfExportFunction() {
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
        this.pdfConverter = new PdfConverter();
    }

    @VisibleForTesting
    PdfExportFunction(PdfExporterPolarionService pdfExporterPolarionService, PdfConverter pdfConverter) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.pdfConverter = pdfConverter;
    }

    @Override
    public void execute(@NotNull ICallContext context, @NotNull IArguments args) {
        if (!(context.getTarget() instanceof IModule module)) {
            return;
        }

        // newly created modules don't have lastRevision and throw UnresolvableObjectException
        // we are going to skip this case entirely, doubt anyone wants to export document that has been just created (thus it is basically empty)
        try {
            module.getLastRevision();
        } catch (UnresolvableObjectException | WrapperException e) {
            return;
        }

        ExportParams exportParams = getExportParams(module, args);
        byte[] pdfBytes = pdfConverter.convertToPdf(exportParams, null);
        savePdfAsWorkItemAttachment(module, exportParams, context.getTargetStatusId(), args, pdfBytes);
    }

    @VisibleForTesting
    @SuppressWarnings("java:S3252") // allow to build ExportParams using its own builder
    ExportParams getExportParams(IModule module, @NotNull IArguments args) {
        String projectId = module.getProjectId();
        String stylePackageName = args.getAsString(PARAM_NAME_STYLE_PACKAGE, STYLE_PACKAGE_DEFAULT);

        String revision = null;
        if (args.getAsBoolean(PARAM_NAME_PREFER_LAST_BASELINE, false)) {
            revision = getLastBaselineRevision(module);
        }

        StylePackageModel stylePackage;
        try {
            stylePackage = ((StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME))
                    .read(ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(stylePackageName), null);
        } catch (ObjectNotFoundException notFoundException) {
            throw new IllegalArgumentException("Styled package '%s' is unavailable. Please contact system administrator.".formatted(stylePackageName));
        }

        return ExportParams.builder()
                .projectId(projectId)
                .locationPath(module.getModuleLocation().getLocationPath())
                .revision(revision)
                .documentType(DocumentType.LIVE_DOC)
                .coverPage(stylePackage.getCoverPage())
                .css(stylePackage.getCss())
                .headerFooter(stylePackage.getHeaderFooter())
                .localization(stylePackage.getLocalization())
                .webhooks(stylePackage.getWebhooks())
                .headersColor(stylePackage.getHeadersColor())
                .orientation(Orientation.valueOf(stylePackage.getOrientation()))
                .paperSize(PaperSize.valueOf(stylePackage.getPaperSize()))
                .fitToPage(stylePackage.isFitToPage())
                .renderComments(stylePackage.getRenderComments())
                .watermark(stylePackage.isWatermark())
                .markReferencedWorkitems(stylePackage.isMarkReferencedWorkitems())
                .cutEmptyChapters(stylePackage.isCutEmptyChapters())
                .cutEmptyWIAttributes(stylePackage.isCutEmptyWorkitemAttributes())
                .cutLocalUrls(stylePackage.isCutLocalURLs())
                .followHTMLPresentationalHints(stylePackage.isFollowHTMLPresentationalHints())
                .numberedListStyles(stylePackage.getCustomNumberedListStyles())
                .chapters(stylePackage.getSpecificChapters() == null ? null : List.of(stylePackage.getSpecificChapters().split(",")))
                .language(stylePackage.getLanguage())
                .linkedWorkitemRoles(stylePackage.getLinkedWorkitemRoles())
                .attachmentsFilter(stylePackage.getAttachmentsFilter())
                .testcaseFieldId(stylePackage.getTestcaseFieldId())
                .build();
    }

    @VisibleForTesting
    void savePdfAsWorkItemAttachment(IModule module, ExportParams exportParams, String targetStatusId, IArguments args, byte[] pdfContentBytes) {
        String workItemProjectId = Objects.requireNonNull(args.getAsString(PARAM_NAME_PROJECT_ID, exportParams.getProjectId()));
        String existingWorkItemId = args.getAsString(PARAM_NAME_EXISTING_WORK_ITEM_ID, null);
        String createWorkItemType = args.getAsString(PARAM_NAME_CREATE_WORK_ITEM_TYPE, null);

        IWorkItem workItem;
        if (existingWorkItemId != null) {
            workItem = pdfExporterPolarionService.getWorkItem(workItemProjectId, existingWorkItemId);
        } else if (createWorkItemType != null) {
            workItem = pdfExporterPolarionService.getTrackerProject(workItemProjectId).createWorkItem(createWorkItemType);
            workItem.setTitle(args.getAsString(PARAM_NAME_CREATE_WORK_ITEM_TITLE, "%s -> %s".formatted(module.getTitleWithSpace(), getStatusName(module, targetStatusId))));
            workItem.setDescription(Text.html(args.getAsString(PARAM_NAME_CREATE_WORK_ITEM_DESCRIPTION, "This item was created automatically. Check 'Attachments' section for the generated PDF document.")));
            workItem.save();
        } else {
            throw new UserFriendlyRuntimeException("Workflow function isn't configured properly. Please contact system administrator.");
        }

        String attachmentFileName = getDocumentFileName(exportParams);
        String attachmentTitle = args.getAsString(PARAM_NAME_ATTACHMENT_TITLE, attachmentFileName.replaceAll("\\.pdf$", ""));
        IAttachment attachment = workItem.createAttachment(attachmentFileName, attachmentTitle, new ByteArrayInputStream(pdfContentBytes));
        attachment.save();
    }

    @VisibleForTesting
    String getLastBaselineRevision(IModule module) {
        IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) pdfExporterPolarionService.getTrackerService().getTrackerProject(module.getProject().getId()).getBaselinesManager();
        return new BaseObjectBaselinesSearch(module, baselinesManager)
                .includeProjectBaselines(true).resolve(true).execute().stream()
                .map(b -> Long.valueOf(b.getBaseRevision()))
                .max(Comparator.naturalOrder()).map(String::valueOf).orElse(null);
    }

    @VisibleForTesting
    String getStatusName(IModule module, String targetStatusId) {
        IEnumeration<?> enumeration = pdfExporterPolarionService.getTrackerService().getDataService().getEnumerationForEnumId(
                new EnumType(Objects.requireNonNull(module.getStatus()).getEnumId()),
                PObjectDataProvider.scopeToContextId(new ProjectScope(module.getProjectId())));
        return enumeration.getAllOptions().stream().filter(e -> Objects.equals(e.getId(), targetStatusId)).map(IEnumOption::getName).findFirst().orElse(targetStatusId);
    }

    @VisibleForTesting
    String getDocumentFileName(ExportParams exportParams) {
        return new DocumentFileNameHelper().getDocumentFileName(exportParams);
    }
}
