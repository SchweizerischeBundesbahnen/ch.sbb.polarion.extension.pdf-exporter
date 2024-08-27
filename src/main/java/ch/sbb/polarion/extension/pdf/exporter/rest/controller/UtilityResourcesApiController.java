package ch.sbb.polarion.extension.pdf.exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf.exporter.model.WebhooksStatus;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;

import javax.ws.rs.Path;
import java.util.List;

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
    public String getFileName(String locationPath, String revision, DocumentType documentType, String scope) {
        return polarionService.callPrivileged(() -> super.getFileName(locationPath, revision, documentType, scope));
    }

    @Override
    public WebhooksStatus getWebhooksStatus() {
        return polarionService.callPrivileged(super::getWebhooksStatus);
    }
}
