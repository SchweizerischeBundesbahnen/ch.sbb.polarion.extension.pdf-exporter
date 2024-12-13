package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.ModelObjectProvider;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.UniqueObjectConverter;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.tr.TestRun;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Objects;

@SuppressWarnings("java:S1200")
public class DocumentDataHelper {
    public static final String DOC_REVISION_CUSTOM_FIELD = "docRevision";
    public static final String URL_QUERY_PARAM_LANGUAGE = "language";
    public static final String URL_QUERY_PARAM_ID = "id";

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public DocumentDataHelper(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public DocumentData<IRichPage> getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveReport(project, exportParams, true);
    }

    public DocumentData<IRichPage> getLiveReport(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    RichPage richPage = new ModelObjectProvider(exportParams)
                            .getRichPage(transaction);

                    return new UniqueObjectConverter(richPage)
                            .withExportParams(exportParams)
                            .withContent(withContent)
                            .toDocumentData(transaction);
                }));
    }

    public DocumentData<ITestRun> getTestRun(@NotNull ITrackerProject project, @NotNull ExportParams exportParams) {
        return getTestRun(project, exportParams, true);
    }

    public DocumentData<ITestRun> getTestRun(@NotNull ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    TestRun testRun = new ModelObjectProvider(exportParams)
                            .getTestRun(transaction);

                    return new UniqueObjectConverter(testRun)
                            .withExportParams(exportParams)
                            .withContent(withContent)
                            .toDocumentData(transaction);
                }));
    }

    public DocumentData<IWikiPage> getWikiPage(@Nullable ITrackerProject project, @NotNull ExportParams exportParams) {
        return getWikiPage(project, exportParams, true);
    }

    public DocumentData<IWikiPage> getWikiPage(@Nullable ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    WikiPage wikiPage = new ModelObjectProvider(exportParams)
                            .getWikiPage(transaction);

                    return new UniqueObjectConverter(wikiPage)
                            .withExportParams(exportParams)
                            .withContent(withContent)
                            .toDocumentData(transaction);
                }));
    }

    public DocumentData<IModule> getLiveDoc(@NotNull ITrackerProject project, @NotNull ExportParams exportParams) {
        return getLiveDoc(project, exportParams, true);
    }

    public DocumentData<IModule> getLiveDoc(@NotNull ITrackerProject project, @NotNull ExportParams exportParams, boolean withContent) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    Document document = new ModelObjectProvider(exportParams)
                            .getDocument(transaction);

                    return new UniqueObjectConverter(document)
                            .withExportParams(exportParams)
                            .withContent(withContent)
                            .toDocumentData(transaction);
                }));
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
        return Boolean.TRUE.equals(TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
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

}
