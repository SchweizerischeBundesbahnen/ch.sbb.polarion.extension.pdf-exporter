package ch.sbb.polarion.extension.pdf.exporter.rest.controller;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf.exporter.util.DocumentFileNameHelper;
import ch.sbb.polarion.extension.pdf.exporter.util.EnumValuesProvider;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.persistence.IEnumOption;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

import static ch.sbb.polarion.extension.pdf.exporter.util.placeholder.PlaceholderValues.DOC_LANGUAGE_FIELD;

@Hidden
@Path("/internal")
public class UtilityResourcesInternalController {

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public UtilityResourcesInternalController() {
        pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    public UtilityResourcesInternalController(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    @GET
    @Path("/link-role-names")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Utility resources")
    @Operation(summary = "Gets list of possible WorkItem link role names in specified project scope")
    public List<String> readLinkRoleNames(@Parameter(description = "Project scope in form project/<PROJECT_ID>/") @QueryParam("scope") String scope) {
        ITrackerProject project = pdfExporterPolarionService.getProjectFromScope(scope);
        if (project != null) {
            return EnumValuesProvider.getAllLinkRoleNames(project);
        }
        return Collections.emptyList();
    }

    @GET
    @Path("/document-language")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Utility resources")
    @Operation(summary = "Gets language of specified Polarion's document, defined in its custom field 'docLanguage'")
    public String getDocumentLanguage(@QueryParam("projectId") String projectId, @QueryParam("spaceId") String spaceId,
                                      @QueryParam("documentName") String documentName, @QueryParam("revision") String revision) {
        IModule module = pdfExporterPolarionService.getModule(projectId, spaceId, documentName, revision);
        Object documentLanguageField = module.getCustomField(DOC_LANGUAGE_FIELD);
        return (documentLanguageField instanceof IEnumOption option) ? option.getId() : null;
    }

    @GET
    @Path("/projects/{projectId}/name")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Utility resources")
    @Operation(summary = "Gets name of specified Polarion's project")
    public String getProjectName(@PathParam("projectId") String projectId) {
        return pdfExporterPolarionService.getProject(projectId).getName();
    }

    @GET
    @Path("/export-filename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "Utility resources")
    @Operation(summary = "Gets a filename, prepared with velocity and placeholders")
    public String getFileName(@QueryParam("locationPath") String locationPath, @QueryParam("revision") String revision, @QueryParam("documentType") DocumentType documentType, @QueryParam("scope") String scope) {
        return new DocumentFileNameHelper(pdfExporterPolarionService).getDocumentFileName(locationPath, revision, documentType, scope);
    }
}
