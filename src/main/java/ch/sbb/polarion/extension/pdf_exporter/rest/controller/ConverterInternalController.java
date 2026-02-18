package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService.JobState;
import ch.sbb.polarion.extension.pdf_exporter.converter.PropertiesUtility;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WidthValidationResult;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobDetails;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobStatus;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfWidthValidationService;
import ch.sbb.polarion.extension.pdf_exporter.util.VeraPdfValidationUtils;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.http.HttpStatus;
import org.verapdf.pdfa.results.ValidationResult;

import javax.inject.Singleton;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "PDF Processing")
@SuppressWarnings("java:S1200")
public class ConverterInternalController {

    private static final String EXPORT_FILENAME_HEADER = "Export-Filename";

    private static final String MISSING_WORKITEM_ATTACHMENTS_COUNT = "Missing-WorkItem-Attachments-Count";
    private static final String WORKITEM_IDS_WITH_MISSING_ATTACHMENT = "WorkItem-IDs-With-Missing-Attachment";
    private static final String PDF_VARIANT_COMPLIANT = "PDF-Variant-Compliant";

    private final PdfConverter pdfConverter;
    private final PdfWidthValidationService pdfWidthValidationService;
    private final PdfConverterJobsService pdfConverterJobService;
    private final PropertiesUtility propertiesUtility;
    private final HtmlToPdfConverter htmlToPdfConverter;

    @Context
    private UriInfo uriInfo;

    public ConverterInternalController() {
        this.pdfConverter = new PdfConverter();
        this.pdfWidthValidationService = new PdfWidthValidationService(pdfConverter);
        ISecurityService securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
        this.pdfConverterJobService = new PdfConverterJobsService(pdfConverter, securityService);
        this.propertiesUtility = new PropertiesUtility();
        this.htmlToPdfConverter = new HtmlToPdfConverter();
    }

    @VisibleForTesting
    ConverterInternalController(PdfConverter pdfConverter, PdfWidthValidationService pdfWidthValidationService, PdfConverterJobsService pdfConverterJobService, UriInfo uriInfo, HtmlToPdfConverter htmlToPdfConverter) {
        this.pdfConverter = pdfConverter;
        this.pdfWidthValidationService = pdfWidthValidationService;
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
                            content = {@Content(mediaType = "application/pdf")},
                            headers = {
                                    @Header(name = HttpHeaders.CONTENT_DISPOSITION,
                                            description = "To inform a browser that the response is a downloadable attachment",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = EXPORT_FILENAME_HEADER,
                                            description = "File name for converted PDF document",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = PDF_VARIANT_COMPLIANT,
                                            description = "Boolean value if resulting PDF is compliant to selected PDF variant",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = MISSING_WORKITEM_ATTACHMENTS_COUNT,
                                            description = "Unavailable work item attachments count",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = WORKITEM_IDS_WITH_MISSING_ATTACHMENT,
                                            description = "Work items contained unavailable attachments",
                                            schema = @Schema(implementation = String.class)
                                    )
                            }
                    )
            })
    public Response convertToPdf(ExportParams exportParams) {
        validateExportParameters(exportParams);
        String fileName = getFileName(exportParams);
        byte[] pdfBytes = pdfConverter.convertToPdf(exportParams, null);
        ValidationResult validationResult = VeraPdfValidationUtils.validatePdf(pdfBytes, exportParams);

        Response.ResponseBuilder responseBuilder = Response.ok(pdfBytes)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(EXPORT_FILENAME_HEADER, fileName)
                .header(MISSING_WORKITEM_ATTACHMENTS_COUNT, ExportContext.getWorkItemIDsWithMissingAttachment().size())
                .header(WORKITEM_IDS_WITH_MISSING_ATTACHMENT, ExportContext.getWorkItemIDsWithMissingAttachment());
        if (validationResult != null) {
            responseBuilder.header(PDF_VARIANT_COMPLIANT, validationResult.isCompliant());
        }
        return responseBuilder.build();
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
                            content = {@Content(mediaType = "application/pdf")},
                            headers = {
                                    @Header(name = HttpHeaders.CONTENT_DISPOSITION,
                                            description = "To inform a browser that the response is a downloadable attachment",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = EXPORT_FILENAME_HEADER,
                                            description = "File name for converted PDF document",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = PDF_VARIANT_COMPLIANT,
                                            description = "Boolean value if resulting PDF is compliant to selected PDF variant",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = MISSING_WORKITEM_ATTACHMENTS_COUNT,
                                            description = "Unavailable work item attachments count",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = WORKITEM_IDS_WITH_MISSING_ATTACHMENT,
                                            description = "Work items contained unavailable attachments",
                                            schema = @Schema(implementation = String.class)
                                    )

                            }
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
        List<String> workItemIDsWithMissingAttachment = pdfConverterJobService.getJobContext(jobId).workItemIDsWithMissingAttachment();
        String fileName = getFileName(exportParams);
        byte[] pdfBytes =  pdfContent.get();
        ValidationResult validationResult = VeraPdfValidationUtils.validatePdf(pdfBytes, exportParams);

        Response.ResponseBuilder responseBuilder = Response.ok(pdfContent.get())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(EXPORT_FILENAME_HEADER, fileName);

        if (validationResult != null) {
            responseBuilder.header(PDF_VARIANT_COMPLIANT, validationResult.isCompliant());
        }

        if (!workItemIDsWithMissingAttachment.isEmpty()) {
            responseBuilder.header(
                    MISSING_WORKITEM_ATTACHMENTS_COUNT,
                    workItemIDsWithMissingAttachment.size()
            );
            responseBuilder.header(
                    WORKITEM_IDS_WITH_MISSING_ATTACHMENT,
                    workItemIDsWithMissingAttachment
            );
        }
        return responseBuilder.build();
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
                            content = {@Content(mediaType = "application/pdf")},
                            headers = {
                                    @Header(name = HttpHeaders.CONTENT_DISPOSITION,
                                            description = "To inform a browser that the response is a downloadable attachment",
                                            schema = @Schema(implementation = String.class)
                                    ),
                                    @Header(name = EXPORT_FILENAME_HEADER,
                                            description = "File name for converted PDF document",
                                            schema = @Schema(implementation = String.class)
                                    )
                            }
                    )
            })
    public Response convertHtmlToPdf(
            @Parameter(description = "input html (must include html and body elements)") String html,
            @Parameter(description = "default value: portrait") @QueryParam("orientation") Orientation orientation,
            @Parameter(description = "default value: A4") @QueryParam("paperSize") PaperSize paperSize,
            @Parameter(description = "default value: pdf/a-2b") @QueryParam("pdfVariant") PdfVariant pdfVariant,
            @Parameter(description = "default value: false") @QueryParam("fitToPage") Boolean fitToPage,
            @Parameter(description = "default value: document.pdf") @QueryParam("fileName") String fileName) {
        ConversionParams.ConversionParamsBuilder<?, ?> conversionParamsBuilder = ConversionParams.builder();
        if (orientation != null) {
            conversionParamsBuilder.orientation(orientation);
        }
        if (paperSize != null) {
            conversionParamsBuilder.paperSize(paperSize);
        }
        if (pdfVariant != null) {
            conversionParamsBuilder.pdfVariant(pdfVariant);
        }
        if (fitToPage != null) {
            conversionParamsBuilder.fitToPage(fitToPage);
        }
        if (fileName != null) {
            conversionParamsBuilder.fileName(fileName);
        }
        conversionParamsBuilder.followHTMLPresentationalHints(true);
        ConversionParams conversionParams = conversionParamsBuilder.build();

        byte[] pdfBytes = htmlToPdfConverter.convert(html, conversionParams);
        String headerFileName = (fileName != null) ? fileName : "document.pdf";
        return Response.ok(pdfBytes)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + headerFileName)
                .header(EXPORT_FILENAME_HEADER, headerFileName)
                .build();
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
        validateExportParameters(exportParams);
        return pdfWidthValidationService.validateWidth(exportParams, maxResults);
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
        if (exportParams.getDocumentType() == DocumentType.BASELINE_COLLECTION) {
            throw new BadRequestException("Parameter 'documentType' should not be 'BASELINE_COLLECTION'");
        }
    }

    private String getFileName(@Nullable ExportParams exportParams) {
        if (exportParams != null) {
            return StringUtils.isEmpty(exportParams.getFileName())
                    ? new DocumentFileNameHelper().getDocumentFileName(exportParams)
                    : exportParams.getFileName();
        } else {
            return "document.pdf";
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
