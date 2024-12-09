package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.WikiRenderer;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.model.tr.TestRun;
import com.polarion.alm.shared.api.model.tr.TestRunReference;
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
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("java:S1200")
public class DocumentDataHelper {
    private static final String DOC_REVISION_CUSTOM_FIELD = "docRevision";
    private static final String URL_QUERY_PARAM_LANGUAGE = "language";
    private static final String URL_QUERY_PARAM_ID = "id";

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public DocumentDataHelper(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public DocumentData<IRichPage> getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveReport(project, exportParams, true);
    }

    public DocumentData<IRichPage> getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> pdfExporterPolarionService.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    String projectId = project != null ? project.getId() : "";
                    String locationPath = exportParams.getLocationPath();
                    if (locationPath == null) {
                        throw new IllegalArgumentException("Location path is required for export");
                    }

                    RichPageReference richPageReference = RichPageReference.fromPath(createPath(projectId, locationPath));
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

                    return DocumentData.builder(DocumentType.LIVE_REPORT, richPage.getOldApi())
                            .projectName(project != null ? project.getName() : "")
                            .lastRevision(richPage.getOldApi().getLastRevision())
                            .baselineName(project != null ? getRevisionBaseline(projectId, richPage.getOldApi(), exportParams.getRevision()) : "")
                            .id(richPage.getOldApi().getId())
                            .title(richPage.getOldApi().getTitleOrName())
                            .content(documentContent)
                            .build();
                }));
    }

    public DocumentData<ITestRun> getTestRun(@NotNull ITrackerProject project, @NotNull ExportParams exportParams) {
        return getTestRun(project, exportParams, true);
    }

    public DocumentData<ITestRun> getTestRun(@NotNull ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> pdfExporterPolarionService.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    String projectId = project.getId();
                    Map<String, String> urlQueryParameters = exportParams.getUrlQueryParameters();
                    if (urlQueryParameters == null || !urlQueryParameters.containsKey(URL_QUERY_PARAM_ID)) {
                        throw new IllegalArgumentException("TestRun id is required for export");
                    }

                    TestRunReference testRunReference = TestRunReference.fromPath(createPath(projectId, urlQueryParameters.get(URL_QUERY_PARAM_ID)));
                    if (exportParams.getRevision() != null) {
                        testRunReference = testRunReference.getWithRevision(exportParams.getRevision());
                    }

                    TestRun testRun = testRunReference.getOriginal(transaction);

                    String documentContent = null;
                    if (withContent) {
                        String html = RpeModelAspect.getPageHtml(testRun);
                        RpeRenderer richPageRenderer = new RpeRenderer((InternalReadOnlyTransaction) transaction, html, RichTextRenderTarget.PDF_EXPORT, testRunReference, testRunReference.scope(), new StrictMapImpl<>());
                        documentContent = richPageRenderer.render(null);
                    }

                    return DocumentData.builder(DocumentType.TEST_RUN, testRun.getOldApi())
                            .projectName(project.getName())
                            .lastRevision(testRun.getOldApi().getLastRevision())
                            .baselineName(getRevisionBaseline(projectId, testRun.getOldApi(), exportParams.getRevision()))
                            .id(testRun.getOldApi().getId())
                            .title(testRun.getOldApi().getLabel())
                            .content(documentContent)
                            .build();
                }));
    }

    public DocumentData<IWikiPage> getWikiPage(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getWikiPage(project, exportParams, true);
    }

    public DocumentData<IWikiPage> getWikiPage(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> pdfExporterPolarionService.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    String projectId = project != null ? project.getId() : "";
                    String locationPath = exportParams.getLocationPath();
                    if (locationPath == null) {
                        throw new IllegalArgumentException("Location path is required for export");
                    }

                    WikiPageReference wikiPageReference = WikiPageReference.fromPath(createPath(projectId, exportParams.getLocationPath()));
                    if (exportParams.getRevision() != null) {
                        wikiPageReference = wikiPageReference.getWithRevision(exportParams.getRevision());
                    }

                    WikiPage wikiPage = wikiPageReference.getOriginal(transaction);

                    String documentContent = null;
                    if (withContent) {
                        documentContent = new WikiRenderer().render(projectId, exportParams.getLocationPath(), exportParams.getRevision());
                    }

                    return DocumentData.builder(DocumentType.WIKI_PAGE, wikiPage.getOldApi())
                            .projectName(project != null ? project.getName() : "")
                            .lastRevision(wikiPage.getOldApi().getLastRevision())
                            .baselineName(project != null ? getRevisionBaseline(projectId, wikiPage.getOldApi(), exportParams.getRevision()) : "")
                            .id(wikiPage.getOldApi().getId())
                            .title(wikiPage.getOldApi().getTitleOrName())
                            .content(documentContent)
                            .build();
                }));
    }

    public DocumentData<IModule> getLiveDoc(@NotNull ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveDoc(project, exportParams, true);
    }

    public DocumentData<IModule> getLiveDoc(@NotNull ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> pdfExporterPolarionService.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    String locationPath = exportParams.getLocationPath();
                    if (locationPath == null) {
                        throw new IllegalArgumentException("Location path is required for export");
                    }

                    DocumentReference documentReference = DocumentReference.fromPath(createPath(project.getId(), exportParams.getLocationPath()));
                    if (exportParams.getRevision() != null) {
                        documentReference = documentReference.getWithRevision(exportParams.getRevision());
                    }

                    Document document = documentReference.getOriginal(transaction);

                    String documentContent = null;
                    if (withContent) {
                        ProxyDocument proxyDocument = new ProxyDocument(document.getOldApi(), (InternalReadOnlyTransaction) transaction);
                        documentContent = getLiveDocContent(exportParams, proxyDocument, (InternalReadOnlyTransaction) transaction);
                    }

                    return DocumentData.builder(DocumentType.LIVE_DOC, document.getOldApi())
                            .projectName(project.getName())
                            .lastRevision(document.getOldApi().getLastRevision())
                            .baselineName(getRevisionBaseline(project.getId(), document.getOldApi(), exportParams.getRevision()))
                            .id(document.getOldApi().getModuleName())
                            .title(document.getOldApi().getTitleOrName())
                            .content(documentContent)
                            .build();
                }));
    }

    public String getLiveDocContent(@NotNull ExportParams exportParams, @NotNull ProxyDocument document, @NotNull InternalReadOnlyTransaction transaction) {
        String internalContent = exportParams.getInternalContent() != null ? exportParams.getInternalContent() : document.getHomePageContentHtml();
        if (internalContent != null && exportParams.isEnableCommentsRendering()) {
            // Add inline comments into document content
            internalContent = new LiveDocCommentsProcessor().addLiveDocComments(document, internalContent);
        }
        Map<String, String> documentParameters = exportParams.getUrlQueryParameters() == null ? Map.of() : exportParams.getUrlQueryParameters();
        DocumentRendererParameters parameters = new DocumentRendererParameters(null, documentParameters.get(URL_QUERY_PARAM_LANGUAGE));
        ModifiedDocumentRenderer documentRenderer = new ModifiedDocumentRenderer(transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
        return documentRenderer.render(internalContent != null ? internalContent : "");
    }

    public String getDocumentStatus(String revision, @NotNull DocumentData<? extends IUniqueObject> documentData) {
        String documentStatus = documentData.getLastRevision();
        if (revision != null) {
            documentStatus = revision;
        } else if (documentData.getDocumentObject() instanceof IModule module) {
            Object docRevision = module.getCustomField(DOC_REVISION_CUSTOM_FIELD);
            if (docRevision != null) {
                documentStatus = docRevision.toString();
            }
        }
        return documentStatus;
    }

    public boolean hasLiveDocNestedNumberedLists(@NotNull ExportParams exportParams) {
        return Boolean.TRUE.equals(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> pdfExporterPolarionService.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
            IProject project = pdfExporterPolarionService.getProject(Objects.requireNonNull(exportParams.getProjectId()));
            ILocation location = getDocumentLocation(exportParams.getLocationPath(), exportParams.getRevision());
            ProxyDocument document = new ProxyDocument(pdfExporterPolarionService.getModule(project, location), (InternalReadOnlyTransaction) transaction);

            String internalContent = document.getHomePageContentHtml();
            DocumentRendererParameters parameters = new DocumentRendererParameters(null, null);
            ModifiedDocumentRenderer documentRenderer = new ModifiedDocumentRenderer((InternalReadOnlyTransaction) transaction, document, RichTextRenderTarget.PDF_EXPORT, parameters);
            String renderedContent = documentRenderer.render(internalContent != null ? internalContent : "");

            return new NumberedListsSanitizer().containsNestedNumberedLists(renderedContent);
        })));
    }

    public ILocation getDocumentLocation(String locationPath, @Nullable String revision) {
        return revision == null ? Location.getLocation(locationPath) : Location.getLocationWithRevision(locationPath, revision);
    }

    @VisibleForTesting
    static @NotNull String createPath(@NotNull String projectId, @NotNull String locationPath) {
        return String.format("%s/%s", projectId, locationPath);
    }

    @NotNull
    private String getRevisionBaseline(@NotNull String projectId, @NotNull IPObject iPObject, @Nullable String revision) {
        IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) pdfExporterPolarionService.getTrackerProject(projectId).getBaselinesManager();
        revision = revision == null ? iPObject.getLastRevision() : revision;

        StringBuilder baselineNameBuilder = new StringBuilder();

        IBaseline projectBaseline = baselinesManager.getRevisionBaseline(revision);
        if (projectBaseline != null) {
            baselineNameBuilder.append("pb. ").append(projectBaseline.getName());
        }

        IBaseline moduleBaseline = baselinesManager.getRevisionBaseline(iPObject, revision);
        if (moduleBaseline != null) {
            if (!baselineNameBuilder.isEmpty()) {
                baselineNameBuilder.append(" | ");
            }
            baselineNameBuilder.append("db. ").append(moduleBaseline.getName());
        }

        return baselineNameBuilder.toString();
    }
}
