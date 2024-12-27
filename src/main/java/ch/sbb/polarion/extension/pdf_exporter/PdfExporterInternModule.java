package ch.sbb.polarion.extension.pdf_exporter;

import com.google.inject.AbstractModule;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;

public class PdfExporterInternModule extends AbstractModule {

    @Override
    protected void configure() {
//        PdfExporterPolarionService pdfExporterPolarionService = new PdfExporterPolarionService();
//        PdfConverter pdfConverter = new PdfConverter();
        ISecurityService securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
        bind(ISecurityService.class).toInstance(securityService);

//        bind(PolarionService.class).toInstance(pdfExporterPolarionService);
//        bind(PdfConverter.class).toInstance(pdfConverter);
//        bind(PdfValidationService.class).toInstance(new PdfValidationService(pdfConverter));
//        bind(PdfConverterJobsService.class).toInstance(new PdfConverterJobsService(pdfConverter, securityService));
//        bind(PropertiesUtility.class).toInstance(new PropertiesUtility());
//        bind(HtmlToPdfConverter.class).toInstance(new HtmlToPdfConverter());
    }
}
