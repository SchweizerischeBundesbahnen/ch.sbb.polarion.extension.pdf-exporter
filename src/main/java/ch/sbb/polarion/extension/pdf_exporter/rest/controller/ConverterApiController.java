package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.NestedListsCheck;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WidthValidationResult;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfValidationService;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Secured
@Path("/api")
public class ConverterApiController extends ConverterInternalController {

    private final PdfExporterPolarionService polarionService;

    public ConverterApiController() {
        this.polarionService = new PdfExporterPolarionService();
    }

    @VisibleForTesting
    @SuppressWarnings("squid:S5803")
    ConverterApiController(PdfExporterPolarionService pdfExporterPolarionService,
                           PdfConverter pdfConverter,
                           PdfValidationService pdfValidationService,
                           DocumentDataHelper documentDataHelper,
                           PdfConverterJobsService pdfConverterJobService,
                           UriInfo uriInfo,
                           HtmlToPdfConverter htmlToPdfConverter) {
        super(pdfExporterPolarionService, pdfConverter, pdfValidationService, documentDataHelper, pdfConverterJobService, uriInfo, htmlToPdfConverter);
        this.polarionService = pdfExporterPolarionService;
    }

    @Override
    public Response convertToPdf(ExportParams exportParams) {
        return polarionService.callPrivileged(() -> super.convertToPdf(exportParams));
    }

    @Override
    public String prepareHtmlContent(ExportParams exportParams) {
        return polarionService.callPrivileged(() -> super.prepareHtmlContent(exportParams));
    }

    @Override
    public Response startPdfConverterJob(ExportParams exportParams) {
        // In async case logout inside the filter must be deactivated. Async Job itself will care about logout after finishing
        deactivateLogoutFilter();

        return polarionService.callPrivileged(() -> super.startPdfConverterJob(exportParams));
    }

    @Override
    public Response getPdfConverterJobStatus(String jobId) {
        return polarionService.callPrivileged(() -> super.getPdfConverterJobStatus(jobId));
    }

    @Override
    public Response getPdfConverterJobResult(String jobId) {
        return polarionService.callPrivileged(() -> super.getPdfConverterJobResult(jobId));
    }

    @Override
    public Response convertHtmlToPdf(String html, Orientation orientation, PaperSize paperSize, String fileName) {
        return polarionService.callPrivileged(() -> super.convertHtmlToPdf(html, orientation, paperSize, fileName));
    }

    @Override
    public WidthValidationResult validatePdfWidth(ExportParams exportParams, int maxResults) {
        return polarionService.callPrivileged(() -> super.validatePdfWidth(exportParams, maxResults));
    }

    @Override
    public NestedListsCheck checkNestedLists(ExportParams exportParams) {
        return polarionService.callPrivileged(() -> super.checkNestedLists(exportParams));
    }

    private void deactivateLogoutFilter() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
            RequestContextHolder.setRequestAttributes(requestAttributes);
        }
    }

}
