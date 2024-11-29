package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Hidden
@Path("/internal")
@Tag(name = "TestRuns")
public class TestRunAttachmentsInternalController {

    private static final String FILENAME_HEADER = "Filename";

    private final PdfExporterPolarionService pdfExporterPolarionService;

    public TestRunAttachmentsInternalController() {
        pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    public TestRunAttachmentsInternalController(PdfExporterPolarionService pdfExporterPolarionService) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    @GET
    @Path("/projects/{projectId}/testruns/{testRunId}/attachments")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of attachments of specified workitem using optional filter",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved list of attachments",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TestRunAttachment.class)))
                    )
            }
    )
    public List<TestRunAttachment> getTestRunAttachments(
            @Parameter(description = "Project ID", required = true) @PathParam("projectId") String projectId,
            @Parameter(description = "TestRun ID", required = true) @PathParam("testRunId") String testRunId,
            @Parameter(description = "TestRun revision") @QueryParam("revision") String revision,
            @Parameter(description = "Filename filter for attachment") @QueryParam("filter") String filter
    ) {
        return pdfExporterPolarionService.getTestRunAttachments(projectId, testRunId, revision, filter);
    }

    @GET
    @Path("/projects/{projectId}/testruns/{testRunId}/attachments/{attachmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets attachment of specified workitem",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved attachment",
                            content = @Content(schema = @Schema(implementation = TestRunAttachment.class))
                    )
            }
    )
    public TestRunAttachment getTesRunAttachment(
            @Parameter(description = "Project ID", required = true) @PathParam("projectId") String projectId,
            @Parameter(description = "TestRun ID", required = true) @PathParam("testRunId") String testRunId,
            @Parameter(description = "Attachment ID", required = true) @PathParam("attachmentId") String attachmentId,
            @Parameter(description = "Attachment revision") @QueryParam("revision") String revision
    ) {
        ITestRunAttachment testRunAttachment = pdfExporterPolarionService.getTestRunAttachment(projectId, testRunId, attachmentId, revision);
        return TestRunAttachment.fromAttachment(testRunAttachment);
    }

    @GET
    @Path("/projects/{projectId}/testruns/{testRunId}/attachments/{attachmentId}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Gets content of attachment of specified workitem",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved attachment content")
            }
    )
    public Response getTestRunAttachmentContent(
            @Parameter(description = "Project ID", required = true) @PathParam("projectId") String projectId,
            @Parameter(description = "TestRun ID", required = true) @PathParam("testRunId") String testRunId,
            @Parameter(description = "Attachment ID", required = true) @PathParam("attachmentId") String attachmentId,
            @Parameter(description = "Attachment revision") @QueryParam("revision") String revision
    ) {
        ITestRunAttachment testRunAttachment = pdfExporterPolarionService.getTestRunAttachment(projectId, testRunId, attachmentId, revision);

        return Response.ok(testRunAttachment.getDataStream())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + testRunAttachment.getFileName() + "\"")
                .header(FILENAME_HEADER, testRunAttachment.getFileName())
                .build();
    }
}
