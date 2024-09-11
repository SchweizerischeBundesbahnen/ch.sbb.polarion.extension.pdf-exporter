package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.LocalizationHelper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Hidden
@Path("/internal")
@Tag(name = "Settings", description = "Operations related to PDF-exporter settings management")
@SecurityRequirement(name = "bearerAuth")
public class SettingsInternalController {

    private final PdfExporterPolarionService pdfExporterPolarionService = new PdfExporterPolarionService();
    private Set<String> predefinedCoverPageTemplates;

    @GET
    @Path("/settings/localization/names/{name}/download")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(summary = "Downloads localization values by name of localization settings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Localization file downloaded successfully")
            }
    )
    public Response downloadTranslations(
            @PathParam("name") String name,
            @QueryParam("language") String language,
            @QueryParam("revision") String revision,
            @QueryParam("scope") String scope
    ) {
        final LocalizationSettings localizationSettings = new LocalizationSettings();
        final LocalizationModel localizationModel = localizationSettings.read(scope, SettingId.fromName(name), revision);
        final Map<String, String> localizationMap = localizationModel.getLocalizationMap(language);
        final String xliff = LocalizationHelper.xmlFromMap(language, localizationMap);

        return Response.ok(xliff.getBytes(StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + language + ".xlf")
                .build();
    }

    @POST
    @Path("/settings/localization/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Uploads localization values",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Localization file uploaded successfully")
            }
    )
    public Map<String, String> uploadTranslations(
            @FormDataParam("file") FormDataBodyPart file,
            @QueryParam("language") String language,
            @QueryParam("scope") String scope
    ) {
        file.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String xliff = file.getValueAs(String.class);
        XLIFFReader.validate(xliff, null);
        return LocalizationHelper.getTranslationsMapForLanguage(xliff);
    }

    @GET
    @Path("/settings/cover-page/templates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get list of cover page predefined template names",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template names retrieved successfully")
            }
    )
    public Collection<String> getCoverPageTemplateNames() {
        if (predefinedCoverPageTemplates == null) {
            predefinedCoverPageTemplates = new CoverPageSettings().getPredefinedTemplates();
        }
        return predefinedCoverPageTemplates;
    }

    @POST
    @Path("/settings/cover-page/templates/{template}")
    @Operation(summary = "Persist content of cover page predefined template",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template persisted successfully")
            }
    )
    public void persistCoverPageTemplate(@PathParam("template") String template, @QueryParam("scope") String scope) {
        if (!getCoverPageTemplateNames().contains(template)) {
            throw new NotFoundException(String.format("There's no predefined template with name '%s'", template));
        }

        CoverPageSettings coverPageSettings = new CoverPageSettings();
        Collection<SettingName> persistedNames = coverPageSettings.readNames(scope);
        String nonClashingName = coverPageSettings.getNonClashingName(template, persistedNames);
        CoverPageModel templateModel = coverPageSettings.defaultValuesFor(template);
        templateModel.setName(nonClashingName);
        UUID uuid = UUID.randomUUID();
        coverPageSettings.processImagePaths(templateModel, template, scope, uuid);
        coverPageSettings.save(scope, SettingId.fromId(uuid.toString()), templateModel);
    }

    @DELETE
    @Path("/settings/cover-page/names/{name}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Deletes images in SVN linked to specified cover page within specified scope (global or certain project)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Images deleted successfully")
            }
    )
    public void deleteImages(@PathParam("name") String coverPageName, @QueryParam("scope") @DefaultValue("") String scope) {
        new CoverPageSettings().deleteCoverPageImages(coverPageName, scope);
    }

    @GET
    @Path("/settings/style-package/suitable-names")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = "Settings")
    @Operation(summary = "Get list of style packages suitable for a document",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of style package names")
            }
    )
    public Collection<SettingName> getSuitableStylePackageNames(@QueryParam("projectId") String projectId, @QueryParam("spaceId") String spaceId, @QueryParam("documentName") String documentName) {
        if (spaceId == null || documentName == null) {
            throw new BadRequestException("Parameters 'spaceId' and 'documentName' are required'");
        }
        return pdfExporterPolarionService.getSuitableStylePackages(projectId, spaceId, documentName);
    }

}
