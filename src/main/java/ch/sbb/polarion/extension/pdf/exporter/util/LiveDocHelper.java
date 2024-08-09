package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.util.exporter.ModifiedDocumentRenderer;
import ch.sbb.polarion.extension.pdf.exporter.util.exporter.WikiRenderer;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.model.wiki.WikiPageReference;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.shared.rpe.RpeModelAspect;
import com.polarion.alm.shared.rpe.RpeRenderer;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;

@SuppressWarnings("java:S1200")
public class LiveDocHelper {
    private static final String DOC_REVISION_CUSTOM_FIELD = "docRevision";
    private static final String URL_QUERY_PARAM_LANGUAGE = "language";

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public LiveDocHelper(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public DocumentData getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveReport(project, exportParams, true);
    }

    public DocumentData getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            String projectId = project != null ? project.getId() : "";
            RichPageReference richPageReference = RichPageReference.fromPath(createPath(projectId, exportParams.getLocationPath()));
            if (exportParams.getRevision() != null) {
                richPageReference = richPageReference.getWithRevision(exportParams.getRevision());
            }

            RichPage richPage = richPageReference.getOriginal(transaction);

            String documentContent = null;
            if (withContent) {
                String html = RpeModelAspect.getPageHtml(richPage);
                Map<String, String> liveReportParameters = exportParams.getUrlQueryParameters() == null ? Map.of() : exportParams.getUrlQueryParameters();
                StrictMap<String, String> urlParameters = new StrictMapImpl<>(liveReportParameters);
                RpeRenderer richPageRenderer = new RpeRenderer((InternalReadOnlyTransaction) transaction, html, RichTextRenderTarget.PDF_EXPORT, richPageReference, richPageReference.scope(), urlParameters);
                documentContent = richPageRenderer.render(null);
            }

            return DocumentData.builder()
                    .projectName(project != null ? project.getName() : "")
                    .lastRevision(richPage.getOldApi().getLastRevision())
                    .baselineName(project != null ? getRevisionBaseline(projectId, richPage.getOldApi(), exportParams.getRevision()) : "")
                    .richPage(richPage.getOldApi())
                    .documentId(richPage.getOldApi().getId())
                    .documentTitle(richPage.getOldApi().getTitle())
                    .documentContent(documentContent)
                    .build();
        });
    }

    public DocumentData getWikiDocument(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getWikiDocument(project, exportParams, true);
    }

    public DocumentData getWikiDocument(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {

            String projectId = project != null ? project.getId() : "";
            WikiPageReference wikiPageReference = WikiPageReference.fromPath(createPath(projectId, exportParams.getLocationPath()));
            if (exportParams.getRevision() != null) {
                wikiPageReference = wikiPageReference.getWithRevision(exportParams.getRevision());
            }

            WikiPage wikiPage = wikiPageReference.getOriginal(transaction);

            String documentContent = null;
            if (withContent) {
                documentContent = new WikiRenderer().render(projectId, exportParams.getLocationPath(), exportParams.getRevision());
            }

            return DocumentData.builder()
                    .projectName(project != null ? project.getName() : "")
                    .lastRevision(wikiPage.getOldApi().getLastRevision())
                    .baselineName(project != null ? getRevisionBaseline(projectId, wikiPage.getOldApi(), exportParams.getRevision()) : "")
                    .wikiPage(wikiPage.getOldApi())
                    .documentId(wikiPage.getOldApi().getId())
                    .documentTitle(wikiPage.getOldApi().getTitle())
                    .documentContent(documentContent)
                    .build();
        });
    }

    public DocumentData getLiveDocument(@NotNull ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveDocument(project, exportParams, true);
    }

    public DocumentData getLiveDocument(@NotNull ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {

            DocumentReference documentReference = DocumentReference.fromPath(createPath(project.getId(), exportParams.getLocationPath()));
            if (exportParams.getRevision() != null) {
                documentReference = documentReference.getWithRevision(exportParams.getRevision());
            }

            Document document = documentReference.getOriginal(transaction);

            String documentContent = null;
            if (withContent) {
                ProxyDocument proxyDocument = new ProxyDocument(document.getOldApi(), (InternalReadOnlyTransaction) transaction);
                documentContent = getDocumentContent(exportParams, proxyDocument, (InternalReadOnlyTransaction) transaction);
            }

            return DocumentData.builder()
                    .projectName(project.getName())
                    .lastRevision(document.getOldApi().getLastRevision())
                    .baselineName(getRevisionBaseline(project.getId(), document.getOldApi(), exportParams.getRevision()))
                    .document(document.getOldApi())
                    .documentId(document.getOldApi().getModuleName())
                    .documentTitle(document.getOldApi().getTitleOrName())
                    .documentContent(documentContent)
                    .build();
        });
    }

    public String getDocumentContent(@NotNull ExportParams exportParams, @NotNull ProxyDocument document, @NotNull InternalReadOnlyTransaction transaction) {
        String internalContent = exportParams.getInternalContent() != null ? exportParams.getInternalContent() : document.getHomePageContentHtml();
        if (internalContent != null && exportParams.isEnableCommentsRendering()) {
            // Add inline comments into document content
            internalContent = new LiveDocCommentsProcessor().addLiveDocComments(document, internalContent);
        }
        DocumentRendererParameters parameters = new DocumentRendererParameters(null, exportParams.getUrlQueryParameters().get(URL_QUERY_PARAM_LANGUAGE));
        ModifiedDocumentRenderer documentRenderer = new ModifiedDocumentRenderer(transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
        return documentRenderer.render(internalContent != null ? internalContent : "");
    }

    public String getDocumentStatus(String revision, @NotNull DocumentData documentData) {
        String documentStatus = documentData.lastRevision;
        if (revision != null) {
            documentStatus = revision;
        } else if (documentData.document != null) { //document is null for wiki pages
            Object docRevision = documentData.document.getCustomField(DOC_REVISION_CUSTOM_FIELD);
            if (docRevision != null) {
                documentStatus = docRevision.toString();
            }
        }
        return documentStatus;
    }

    public boolean documentContainsNestedNumberedLists(ExportParams exportParams) {
        return Boolean.TRUE.equals(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IProject project = pdfExporterPolarionService.getProject(exportParams.getProjectId());
            ILocation location = getDocumentLocation(exportParams.getLocationPath(), exportParams.getRevision());
            ProxyDocument document = new ProxyDocument(pdfExporterPolarionService.getModule(project, location), (InternalReadOnlyTransaction) transaction);

            String internalContent = document.getHomePageContentHtml();
            DocumentRendererParameters parameters = new DocumentRendererParameters(null, null);
            ModifiedDocumentRenderer documentRenderer = new ModifiedDocumentRenderer((InternalReadOnlyTransaction) transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
            String renderedContent = documentRenderer.render(internalContent != null ? internalContent : "");

            return new NumberedListsSanitizer().containsNestedNumberedLists(renderedContent);
        }));
    }

    public ILocation getDocumentLocation(String locationPath, String revision) {
        return revision == null ? Location.getLocation(locationPath)
                : Location.getLocationWithRevision(locationPath, revision);
    }

    @VisibleForTesting
    static @NotNull String createPath(@NotNull String projectId, @NotNull String locationPath) {
        return String.format("%s/%s", projectId, locationPath);
    }

    @Nullable
    private String getRevisionBaseline(@NotNull String projectId, @NotNull IPObject iPObject, @Nullable String revision) {
        IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) pdfExporterPolarionService.getTrackerProject(projectId).getBaselinesManager();
        IBaseline baseline = baselinesManager.getRevisionBaseline(iPObject, revision != null ? revision : iPObject.getLastRevision());
        return baseline != null ? baseline.getName() : null;
    }

    @Builder
    @Getter
    public static class DocumentData {
        private String projectName;
        private IModule document;
        private IWikiPage wikiPage;
        private IRichPage richPage;
        private String lastRevision;
        private String baselineName;
        private String documentId;
        private String documentTitle;
        private String documentContent;
    }
}
