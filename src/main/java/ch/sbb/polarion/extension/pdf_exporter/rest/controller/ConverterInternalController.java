package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService.JobState;
import ch.sbb.polarion.extension.pdf_exporter.converter.PropertiesUtility;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.NestedListsCheck;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WidthValidationResult;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobDetails;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobStatus;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfValidationService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.http.HttpStatus;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Hidden
@Path("/internal")
@Tag(name = "PDF Processing")
@SuppressWarnings("java:S1200")
public class ConverterInternalController {

    private final PdfExporterPolarionService pdfExporterPolarionService;
    private final PdfConverter pdfConverter;
    private final PdfValidationService pdfValidationService;
    private final DocumentDataHelper documentDataHelper;
    private final PdfConverterJobsService pdfConverterJobService;
    private final PropertiesUtility propertiesUtility;
    private final HtmlToPdfConverter htmlToPdfConverter;

    @Context
    private UriInfo uriInfo;

    public ConverterInternalController() {
        this.pdfExporterPolarionService = new PdfExporterPolarionService();
        this.pdfConverter = new PdfConverter();
        this.pdfValidationService = new PdfValidationService(pdfConverter);
        this.documentDataHelper = new DocumentDataHelper(pdfExporterPolarionService);
        ISecurityService securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
        this.pdfConverterJobService = new PdfConverterJobsService(pdfConverter, securityService);
        this.propertiesUtility = new PropertiesUtility();
        this.htmlToPdfConverter = new HtmlToPdfConverter();
    }

    @VisibleForTesting
    ConverterInternalController(PdfExporterPolarionService pdfExporterPolarionService,
                                PdfConverter pdfConverter,
                                PdfValidationService pdfValidationService,
                                DocumentDataHelper documentDataHelper,
                                PdfConverterJobsService pdfConverterJobService,
                                UriInfo uriInfo,
                                HtmlToPdfConverter htmlToPdfConverter) {
        this.pdfExporterPolarionService = pdfExporterPolarionService;
        this.pdfConverter = pdfConverter;
        this.pdfValidationService = pdfValidationService;
        this.documentDataHelper = documentDataHelper;
        this.pdfConverterJobService = pdfConverterJobService;
        this.uriInfo = uriInfo;
        this.propertiesUtility = new PropertiesUtility();
        this.htmlToPdfConverter = htmlToPdfConverter;
    }

    @POST
    @Path("/convert")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    @Operation(summary = "Returns requested Polarion's document converted to PDF",
            requestBody = @RequestBody(description = "Export parameters to generate the PDF",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ExportParams.class),
                            mediaType = MediaType.APPLICATION_JSON
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Content of PDF document as a byte array",
                            content = {@Content(mediaType = "application/pdf")}
                    )
            })
    public Response convertToPdf(ExportParams exportParams) {
        validateExportParameters(exportParams);
        String fileName = new DocumentFileNameHelper(pdfExporterPolarionService).getDocumentFileName(exportParams);
        byte[] pdfBytes = pdfConverter.convertToPdf(exportParams, null);
        return Response.ok(pdfBytes).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName).build();
    }

    @POST
    @Path("/prepared-html-content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Returns prepared HTML which will be used for PDF conversion using WeasyPrint",
            requestBody = @RequestBody(description = "Export parameters to generate the PDF",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ExportParams.class),
                            mediaType = MediaType.APPLICATION_JSON
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Prepared HTML content",
                            content = {@Content(mediaType = MediaType.TEXT_HTML)}
                    )
            })
    public String prepareHtmlContent(ExportParams exportParams) {
        validateExportParameters(exportParams);
        return pdfConverter.prepareHtmlContent(exportParams, null);
    }

    @POST
    @Path("/convert/jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Starts asynchronous conversion job of Polarion's document to PDF",
            requestBody = @RequestBody(description = "Export parameters to generate the PDF",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ExportParams.class),
                            mediaType = MediaType.APPLICATION_JSON
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "202",
                            description = "Conversion process is started, job URI is returned in Location header"
                    )
            })
    public Response startPdfConverterJob(ExportParams exportParams) {
        validateExportParameters(exportParams);

        String jobId = pdfConverterJobService.startJob(exportParams, propertiesUtility.getInProgressJobTimeout());

        URI jobUri = UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path(jobId).build();
        return Response.accepted().location(jobUri).build();
    }

    @GET
    @Path("/convert/jobs/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns PDF conversion job status",
            responses = {
                    // OpenAPI response MediaTypes for 303 and 202 response codes are generic to satisfy automatic redirect in SwaggerUI
                    @ApiResponse(responseCode = "303",
                            description = "Conversion job is finished successfully, Location header contains result URL",
                            content = {@Content(mediaType = "application/*", schema = @Schema(implementation = ConverterJobDetails.class))}
                    ),
                    @ApiResponse(responseCode = "202",
                            description = "Conversion job is still in progress",
                            content = {@Content(mediaType = "application/*", schema = @Schema(implementation = ConverterJobDetails.class))}
                    ),
                    @ApiResponse(responseCode = "409",
                            description = "Conversion job is failed or cancelled"
                    ),
                    @ApiResponse(responseCode = "404",
                            description = "Conversion job id is unknown"
                    )
            })
    public Response getPdfConverterJobStatus(@PathParam("id") String jobId) {
        JobState jobState = pdfConverterJobService.getJobState(jobId);

        ConverterJobStatus converterJobStatus = convertToJobStatus(jobState);
        ConverterJobDetails jobDetails = ConverterJobDetails.builder()
                .status(converterJobStatus)
                .errorMessage(jobState.errorMessage()).build();

        Response.ResponseBuilder responseBuilder;
        switch (converterJobStatus) {
            case IN_PROGRESS -> responseBuilder = Response.accepted();
            case SUCCESSFULLY_FINISHED -> {
                URI jobUri = UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path("result").build();
                responseBuilder = Response.status(HttpStatus.SEE_OTHER.value()).location(jobUri);
            }
            default -> responseBuilder = Response.status(HttpStatus.CONFLICT.value());
        }
        return responseBuilder.entity(jobDetails).build();
    }

    @GET
    @Path("/convert/jobs/{id}/result")
    @Produces("application/pdf")
    @Operation(summary = "Returns PDF conversion job result",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Conversion job result is ready",
                            content = {@Content(mediaType = "application/pdf")}
                    ),
                    @ApiResponse(responseCode = "204",
                            description = "Conversion job is still in progress"
                    ),
                    @ApiResponse(responseCode = "409",
                            description = "Conversion job is failed, cancelled or result is unreachable"
                    ),
                    @ApiResponse(responseCode = "404",
                            description = "Conversion job id is unknown"
                    )
            })
    public Response getPdfConverterJobResult(@PathParam("id") String jobId) {
        Optional<byte[]> pdfContent = pdfConverterJobService.getJobResult(jobId);
        if (pdfContent.isEmpty()) {
            return Response.status(HttpStatus.NO_CONTENT.value()).build();
        }
        ExportParams exportParams = pdfConverterJobService.getJobParams(jobId);
        return Response.ok(pdfContent.get()).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportParams.getFileName() + "\"").build();
    }

    @GET
    @Path("/convert/jobs")
    @Produces("application/json")
    @Operation(summary = "Returns all active PDF conversion jobs statuses",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Conversion jobs statuses",
                            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))}
                    )
            })
    public Response getAllPdfConverterJobs() {
        Map<String, JobState> jobsStates = pdfConverterJobService.getAllJobsStates();
        Map<String, ConverterJobDetails> jobsDetails = jobsStates.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        ConverterJobDetails.builder()
                                .status(convertToJobStatus(entry.getValue()))
                                .errorMessage(entry.getValue().errorMessage())
                                .build()));
        return Response.ok(jobsDetails).build();
    }

    @POST
    @Path("/convert/html")
    @Consumes(MediaType.TEXT_HTML)
    @Produces("application/pdf")
    @Operation(summary = "Converts input HTML to PDF",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Content of PDF document as a byte array",
                            content = {@Content(mediaType = "application/pdf")}
                    )
            })
    public Response convertHtmlToPdf(
            @Parameter(description = "input html (must include html and body elements)") String html,
            @Parameter(description = "default value: portrait") @QueryParam("orientation") Orientation orientation,
            @Parameter(description = "default value: A4") @QueryParam("paperSize") PaperSize paperSize,
            @Parameter(description = "default value: document.pdf") @QueryParam("fileName") String fileName) {
        byte[] pdfBytes = htmlToPdfConverter.convert(html, orientation, paperSize);
        String dispositionFileName = (fileName != null) ? fileName : "document.pdf";
        return Response.ok(pdfBytes).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dispositionFileName).build();
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validates if requested Polarion's document been converted to PDF doesn't contain pages which content exceeds page's width",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Validation result",
                            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = WidthValidationResult.class))}
                    )
            })
    public WidthValidationResult validatePdfWidth(
            ExportParams exportParams,
            @Parameter(description = "Limit of 'invalid' pages in response", required = true) @QueryParam("max-results") int maxResults) {
        if (exportParams.getProjectId() == null || exportParams.getLocationPath() == null) {
            throw new BadRequestException("Both 'projectId' and 'locationPath' parameters should be provided to locate a document for validation");
        }
        return pdfValidationService.validateWidth(exportParams, maxResults);
    }

    @POST
    @Path("/checknestedlists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Checks whether document contains nested lists",
            requestBody = @RequestBody(
                    description = "Export parameters used to locate and check the document for nested lists",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ExportParams.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Check completed successfully, returning whether nested lists are present",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = NestedListsCheck.class))
                    )
            }
    )
    @SuppressWarnings("java:S1166")
    public NestedListsCheck checkNestedLists(ExportParams exportParams) {
        boolean containsNestedLists = documentDataHelper.hasLiveDocNestedNumberedLists(exportParams);
        return NestedListsCheck.builder().containsNestedLists(containsNestedLists).build();
    }

    private void validateExportParameters(ExportParams exportParams) {
        if (exportParams == null) {
            throw new BadRequestException("Missing export parameters");
        }
        if (exportParams.getDocumentType() == DocumentType.LIVE_DOC && exportParams.getProjectId() == null) {
            throw new BadRequestException("Parameter 'projectId' should be provided");
        }
        if (exportParams.getLocationPath() == null && exportParams.getDocumentType() != DocumentType.TEST_RUN) {
            throw new BadRequestException("Parameter 'locationPath' should be provided");
        }
    }

    private ConverterJobStatus convertToJobStatus(JobState jobState) {
        if (!jobState.isDone()) {
            return ConverterJobStatus.IN_PROGRESS;
        } else if (!jobState.isCancelled() && !jobState.isCompletedExceptionally()) {
            return ConverterJobStatus.SUCCESSFULLY_FINISHED;
        } else if (jobState.isCancelled()) {
            return ConverterJobStatus.CANCELLED;
        } else {
            return ConverterJobStatus.FAILED;
        }
    }
}
