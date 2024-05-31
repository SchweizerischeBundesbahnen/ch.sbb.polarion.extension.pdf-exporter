package ch.sbb.polarion.extension.pdf.exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.ConfigurationStatus;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.Path;

@Secured
@Path("/api")
public class ConfigurationApiController extends ConfigurationInternalController {

    private static final PolarionService polarionService = new PolarionService();

    @Override
    public @NotNull ConfigurationStatus checkDefaultSettings(String scope) {
        return polarionService.callPrivileged(() -> super.checkDefaultSettings(scope));
    }

    @Override
    public @NotNull ConfigurationStatus checkDocumentPropertiesPaneConfig(String scope) {
        return polarionService.callPrivileged(() -> super.checkDocumentPropertiesPaneConfig(scope));
    }

    @Override
    public @NotNull ConfigurationStatus checkDleToolbarConfig() {
        return polarionService.callPrivileged(super::checkDleToolbarConfig);
    }

    @Override
    public @NotNull ConfigurationStatus checkLiveReportConfig() {
        return polarionService.callPrivileged(super::checkLiveReportConfig);
    }

    @Override
    public @NotNull ConfigurationStatus checkCORSConfig() {
        return polarionService.callPrivileged(super::checkCORSConfig);
    }

    @Override
    public @NotNull ConfigurationStatus checkWeasyPrint() {
        return polarionService.callPrivileged(super::checkWeasyPrint);
    }

}
