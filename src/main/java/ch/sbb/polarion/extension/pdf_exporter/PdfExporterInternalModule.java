package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import com.google.inject.AbstractModule;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import com.polarion.portal.internal.server.navigation.TestManagementServiceAccessor;

public class PdfExporterInternalModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ISecurityService.class).toInstance(PlatformContext.getPlatform().lookupService(ISecurityService.class));
        bind(ITestManagementService.class).toInstance(new TestManagementServiceAccessor().getTestingService());
        bind(FileResourceProvider.class).to(PdfExporterFileResourceProvider.class);
    }
}
