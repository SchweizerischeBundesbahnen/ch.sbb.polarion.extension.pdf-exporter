package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf_exporter.model.WebhooksStatus;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import java.util.List;

@Singleton
@Secured
@Path("/api")
public class UtilityResourcesApiController extends UtilityResourcesInternalController {

    private static final PolarionService polarionService = new PolarionService();

    @Override
    public List<String> readLinkRoleNames(String scope) {
        return polarionService.callPrivileged(() -> super.readLinkRoleNames(scope));
    }

    @Override
    public String getDocumentLanguage(String projectId, String spaceId, String documentName, String revision) {
        return polarionService.callPrivileged(() -> super.getDocumentLanguage(projectId, spaceId, documentName, revision));
    }

    @Override
    public String getProjectName(String projectId) {
        return polarionService.callPrivileged(() -> super.getProjectName(projectId));
    }

    @Override
    public String getFileName(ExportParams exportParams) {
        return polarionService.callPrivileged(() -> super.getFileName(exportParams));
    }

    @Override
    public WebhooksStatus getWebhooksStatus() {
        return polarionService.callPrivileged(super::getWebhooksStatus);
    }
}
