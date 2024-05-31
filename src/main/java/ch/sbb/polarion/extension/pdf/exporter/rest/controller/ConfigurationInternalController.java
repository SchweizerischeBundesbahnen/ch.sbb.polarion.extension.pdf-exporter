package ch.sbb.polarion.extension.pdf.exporter.rest.controller;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.pdf.exporter.util.configuration.ConfigurationStatusUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Hidden
@Path("/internal")
public class ConfigurationInternalController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/default-settings")
    @Operation(summary = "Checks default settings configuration")
    public @NotNull ConfigurationStatus checkDefaultSettings(@QueryParam("scope") @DefaultValue("") String scope) {
        return ConfigurationStatusUtils.getSettingsStatus(scope);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/document-properties-pane-config")
    @Operation(summary = "Checks document properties pane configuration")
    public @NotNull ConfigurationStatus checkDocumentPropertiesPaneConfig(@QueryParam("scope") @DefaultValue("") String scope) {
        return ConfigurationStatusUtils.getDocumentPropertiesPaneStatus(scope);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/dle-toolbar-config")
    @Operation(summary = "Checks DLE Toolbar configuration")
    public @NotNull ConfigurationStatus checkDleToolbarConfig() {
        return ConfigurationStatusUtils.getDleToolbarStatus();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/live-report-config")
    @Operation(summary = "Checks Live Report configuration")
    public @NotNull ConfigurationStatus checkLiveReportConfig() {
        return ConfigurationStatusUtils.getLiveReportMainHeadStatus();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/cors-config")
    @Operation(summary = "Checks CORS configuration")
    public @NotNull ConfigurationStatus checkCORSConfig() {
        return ConfigurationStatusUtils.getCORSStatus();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/weasyprint")
    @Operation(summary = "Checks WeasyPrint configuration")
    public @NotNull ConfigurationStatus checkWeasyPrint() {
        return ConfigurationStatusUtils.getWeasyPrintStatus();
    }
}
