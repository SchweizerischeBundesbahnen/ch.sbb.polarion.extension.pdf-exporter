package ch.sbb.polarion.extension.pdf.exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;

@Secured
@Path("/api")
public class SettingsApiController extends SettingsInternalController {

    private static final PolarionService polarionService = new PolarionService();

    @Override
    public Response downloadTranslations(String name, String language, String revision, String scope) {
        return polarionService.callPrivileged(() -> super.downloadTranslations(name, language, revision, scope));
    }

    @Override
    public Map<String, String> uploadTranslations(FormDataBodyPart file, String language, String scope) {
        return polarionService.callPrivileged(() -> super.uploadTranslations(file, language, scope));
    }

    @Override
    public Collection<String> getCoverPageTemplateNames() {
        return polarionService.callPrivileged(super::getCoverPageTemplateNames);
    }

    @Override
    public void persistCoverPageTemplate(String template, String scope) {
        polarionService.callPrivileged(() -> super.persistCoverPageTemplate(template, scope));
    }

    @Override
    public void deleteImages(String coverPageName, String scope) {
        polarionService.callPrivileged(() -> super.deleteImages(coverPageName, scope));
    }
}
