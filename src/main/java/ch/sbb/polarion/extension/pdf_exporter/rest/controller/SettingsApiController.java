package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import com.google.inject.Inject;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Secured
@Path("/api")
public class SettingsApiController extends SettingsInternalController {

    private static final PolarionService polarionService = new PolarionService();

    @Inject
    public SettingsApiController(PdfExporterPolarionService pdfExporterPolarionService, CoverPageSettings coverPageSettings) {
        super(pdfExporterPolarionService, coverPageSettings);
    }

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

    @Override
    public Collection<SettingName> getSuitableStylePackageNames(String projectId, String spaceId, String documentName) {
        return polarionService.callPrivileged(() -> super.getSuitableStylePackageNames(projectId, spaceId, documentName));
    }

    @Override
    public Collection<StylePackageWeightInfo> getStylePackageWeights(String scope) {
        return polarionService.callPrivileged(() -> super.getStylePackageWeights(scope));
    }

    @Override
    public void updateStylePackageWeights(List<StylePackageWeightInfo> stylePackageWeights) {
        polarionService.callPrivileged(() -> super.updateStylePackageWeights(stylePackageWeights));
    }
}
