package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.model.tr.TestRun;
import com.polarion.alm.shared.api.model.tr.TestRunReference;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.model.wiki.WikiPageReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class ModelObjectProvider {
    public static final String URL_QUERY_PARAM_ID = "id";

    private final @NotNull ExportParams exportParams;
    private final @NotNull PdfExporterPolarionService pdfExporterPolarionService;

    public ModelObjectProvider(@NotNull ExportParams exportParams) {
        this.exportParams = exportParams;
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    public ModelObjectProvider(@NotNull ExportParams exportParams, @NotNull PdfExporterPolarionService pdfExporterPolarionService) {
        this.exportParams = exportParams;
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public ModelObject getModelObject(@NotNull DocumentType documentType, ReadOnlyTransaction transaction) {
        return switch (exportParams.getDocumentType()) {
            case LIVE_DOC -> getDocument(transaction);
            case LIVE_REPORT -> getRichPage(transaction);
            case TEST_RUN -> getTestRun(transaction);
            case WIKI_PAGE -> getWikiPage(transaction);
            case BASELINE_COLLECTION -> throw new IllegalArgumentException("Unsupported document type: %s".formatted(exportParams.getDocumentType()));
        };
    }

    public @NotNull RichPage getRichPage(@NotNull ReadOnlyTransaction transaction) {
        String projectId = getProjectId().orElse("");
        String locationPath = getLocationPath().orElseThrow(() -> new IllegalArgumentException("Location path is required for export"));

        RichPageReference richPageReference = RichPageReference.fromPath(createPath(projectId, locationPath));
        if (exportParams.getRevision() != null) {
            richPageReference = richPageReference.getWithRevision(exportParams.getRevision());
        }

        return richPageReference.getOriginal(transaction);
    }

    public @NotNull TestRun getTestRun(ReadOnlyTransaction transaction) {
        String projectId = getProjectId().orElseThrow(() -> new IllegalArgumentException("Project id is required for export"));
        String testRunId = getTestRunId().orElseThrow(() -> new IllegalArgumentException("Test run id is required for export"));

        TestRunReference testRunReference = TestRunReference.fromPath(createPath(projectId, testRunId));
        if (exportParams.getRevision() != null) {
            testRunReference = testRunReference.getWithRevision(exportParams.getRevision());
        }

        return testRunReference.getOriginal(transaction);
    }

    public @NotNull WikiPage getWikiPage(@NotNull ReadOnlyTransaction transaction) {
        String projectId = getProjectId().orElse("");
        String locationPath = getLocationPath().orElseThrow(() -> new IllegalArgumentException("Location path is required for export"));

        WikiPageReference wikiPageReference = WikiPageReference.fromPath(createPath(projectId, locationPath));
        if (exportParams.getRevision() != null) {
            wikiPageReference = wikiPageReference.getWithRevision(exportParams.getRevision());
        }

        return wikiPageReference.getOriginal(transaction);
    }

    public Document getDocument(ReadOnlyTransaction transaction) {
        String projectId = getProjectId().orElseThrow(() -> new IllegalArgumentException("Project id is required for export"));
        String locationPath = getLocationPath().orElseThrow(() -> new IllegalArgumentException("Location path is required for export"));

        DocumentReference documentReference = DocumentReference.fromPath(createPath(projectId, locationPath));
        if (exportParams.getRevision() != null) {
            documentReference = documentReference.getWithRevision(exportParams.getRevision());
        }

        return documentReference.getOriginal(transaction);
    }


    private Optional<String> getProjectId() {
        IProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getProject(exportParams.getProjectId());
        }
        String projectId = project != null ? project.getId() : null;
        return Optional.ofNullable(projectId);
    }

    private Optional<String> getLocationPath() {
        return Optional.ofNullable(exportParams.getLocationPath());
    }

    private Optional<String> getTestRunId() {
        Map<String, String> urlQueryParameters = exportParams.getUrlQueryParameters();
        if (urlQueryParameters == null || !urlQueryParameters.containsKey(URL_QUERY_PARAM_ID)) {
            return Optional.empty();
        }
        return Optional.ofNullable(urlQueryParameters.get(URL_QUERY_PARAM_ID));
    }

    public static @NotNull String createPath(@NotNull String projectId, @NotNull String locationPath) {
        return String.format("%s/%s", projectId, locationPath);
    }

}
