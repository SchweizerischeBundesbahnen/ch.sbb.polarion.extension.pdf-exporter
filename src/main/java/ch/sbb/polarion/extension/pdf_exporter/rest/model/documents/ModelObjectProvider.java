package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.projects.model.IProject;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataHelper.URL_QUERY_PARAM_ID;

public class ModelObjectProvider {

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

    public @NotNull RichPage getRichPage(@NotNull ReadOnlyTransaction transaction) {
        IProject project = getProject(exportParams);
        String projectId = project != null ? project.getId() : "";
        String locationPath = exportParams.getLocationPath();
        if (locationPath == null) {
            throw new IllegalArgumentException("Location path is required for export");
        }

        RichPageReference richPageReference = RichPageReference.fromPath(createPath(projectId, locationPath));
        if (exportParams.getRevision() != null) {
            richPageReference = richPageReference.getWithRevision(exportParams.getRevision());
        }

        return richPageReference.getOriginal(transaction);
    }

    public @NotNull TestRun getTestRun(ReadOnlyTransaction transaction) {
        IProject project = getProject(exportParams);
        if (project == null) {
            throw new IllegalArgumentException("Project id is required for export");
        }
        Map<String, String> urlQueryParameters = exportParams.getUrlQueryParameters();
        if (urlQueryParameters == null || !urlQueryParameters.containsKey(URL_QUERY_PARAM_ID)) {
            throw new IllegalArgumentException("TestRun id is required for export of test run");
        }

        TestRunReference testRunReference = TestRunReference.fromPath(createPath(project.getId(), urlQueryParameters.get(URL_QUERY_PARAM_ID)));
        if (exportParams.getRevision() != null) {
            testRunReference = testRunReference.getWithRevision(exportParams.getRevision());
        }

        return testRunReference.getOriginal(transaction);
    }


    public @NotNull WikiPage getWikiPage(@NotNull ReadOnlyTransaction transaction) {
        IProject project = getProject(exportParams);
        String projectId = project != null ? project.getId() : "";
        String locationPath = exportParams.getLocationPath();
        if (locationPath == null) {
            throw new IllegalArgumentException("Location path is required for export");
        }

        WikiPageReference wikiPageReference = WikiPageReference.fromPath(createPath(projectId, exportParams.getLocationPath()));
        if (exportParams.getRevision() != null) {
            wikiPageReference = wikiPageReference.getWithRevision(exportParams.getRevision());
        }

        return wikiPageReference.getOriginal(transaction);
    }

    public Document getDocument(ReadOnlyTransaction transaction) {
        IProject project = getProject(exportParams);
        if (project == null) {
            throw new IllegalArgumentException("Project id is required for export");
        }
        String locationPath = exportParams.getLocationPath();
        if (locationPath == null) {
            throw new IllegalArgumentException("Location path is required for export");
        }

        DocumentReference documentReference = DocumentReference.fromPath(createPath(project.getId(), exportParams.getLocationPath()));
        if (exportParams.getRevision() != null) {
            documentReference = documentReference.getWithRevision(exportParams.getRevision());
        }

        return documentReference.getOriginal(transaction);
    }


    private @Nullable IProject getProject(@NotNull ExportParams exportParams) {
        IProject project = null;
        if (!StringUtils.isEmpty(exportParams.getProjectId())) {
            project = pdfExporterPolarionService.getProject(exportParams.getProjectId());
        }
        return project;
    }

    public static @NotNull String createPath(@NotNull String projectId, @NotNull String locationPath) {
        return String.format("%s/%s", projectId, locationPath);
    }

}
