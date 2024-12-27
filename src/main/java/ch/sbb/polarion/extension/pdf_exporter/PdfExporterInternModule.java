package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.converter.PropertiesUtility;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfValidationService;
import com.google.inject.AbstractModule;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;

public class PdfExporterInternModule extends AbstractModule {

    @Override
    protected void configure() {
        PdfExporterPolarionService pdfExporterPolarionService = new PdfExporterPolarionService();
        PdfConverter pdfConverter = new PdfConverter();
        ISecurityService securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);

        bind(PolarionService.class).toInstance(pdfExporterPolarionService);
        bind(PdfConverter.class).toInstance(pdfConverter);
        bind(ISecurityService.class).toInstance(securityService);
        bind(PdfValidationService.class).toInstance(new PdfValidationService(pdfConverter));
        bind(PdfConverterJobsService.class).toInstance(new PdfConverterJobsService(pdfConverter, securityService));
        bind(PropertiesUtility.class).toInstance(new PropertiesUtility());
        bind(HtmlToPdfConverter.class).toInstance(new HtmlToPdfConverter());
    }
}
