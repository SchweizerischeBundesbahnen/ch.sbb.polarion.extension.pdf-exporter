package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.model.WebhooksStatus;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.EnumValuesProvider;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.platform.persistence.IEnumOption;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

import static ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderValues.DOC_LANGUAGE_FIELD;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Utility resources")
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
    @Operation(summary = "Gets list of possible WorkItem link role names in specified project scope",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved list of link role names",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))
                    )
            }
    )
    public List<String> readLinkRoleNames(@Parameter(description = "Project scope in form project/<PROJECT_ID>/", required = true) @QueryParam("scope") String scope) {
        ITrackerProject project = pdfExporterPolarionService.getProjectFromScope(scope);
        if (project != null) {
            return EnumValuesProvider.getAllLinkRoleNames(project);
        }
        return Collections.emptyList();
    }

    @GET
    @Path("/document-language")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Gets language of specified Polarion's document, defined in its custom field 'docLanguage'",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved document language")
            }
    )
    public String getDocumentLanguage(@QueryParam("projectId") String projectId, @QueryParam("spaceId") String spaceId,
                                      @QueryParam("documentName") String documentName, @QueryParam("revision") String revision) {
        IModule module = pdfExporterPolarionService.getModule(projectId, spaceId, documentName, revision);
        Object documentLanguageField = module.getCustomField(DOC_LANGUAGE_FIELD);
        return (documentLanguageField instanceof IEnumOption option) ? option.getId() : null;
    }

    @GET
    @Path("/projects/{projectId}/name")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Gets name of specified Polarion's project",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved project name")
            }
    )
    public String getProjectName(@PathParam("projectId") String projectId) {
        return pdfExporterPolarionService.getProject(projectId).getName();
    }

    @POST
    @Path("/export-filename")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Gets a filename, prepared with velocity and placeholders",
            requestBody = @RequestBody(description = "Export parameters similar as for generation of PDF",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ExportParams.class),
                            mediaType = MediaType.APPLICATION_JSON
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully generated filename")
            }
    )
    public String getFileName(ExportParams exportParams) {
        return new DocumentFileNameHelper().getDocumentFileName(exportParams);
    }

    @GET
    @Path("/webhooks/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Utility resources")
    @Operation(summary = "Gets webhooks status - if they are enabled or not",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Webhooks status",
                            content = @Content(schema = @Schema(implementation = WebhooksStatus.class)))
            }
    )
    public WebhooksStatus getWebhooksStatus() {
        Boolean webhooksEnabled = PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled();
        return WebhooksStatus.builder().enabled(webhooksEnabled).build();
    }
}
