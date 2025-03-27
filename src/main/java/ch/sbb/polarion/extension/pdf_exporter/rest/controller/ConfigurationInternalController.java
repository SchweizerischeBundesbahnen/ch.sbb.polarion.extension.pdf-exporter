package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.CORSStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.DefaultSettingsStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.DleToolbarStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.DocumentPropertiesPaneStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.LiveReportMainHeadStatusProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.configuration.WeasyPrintStatusProvider;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Hidden
@Path("/internal")
@Tag(name = "Configuration status")
public class ConfigurationInternalController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/default-settings")
    @Operation(
            summary = "Checks default settings configuration",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved default settings configuration",
                            content = @Content(schema = @Schema(implementation = ConfigurationStatus.class))
                    )
            }
    )
    public @NotNull ConfigurationStatus checkDefaultSettings(@QueryParam("scope") @DefaultValue("") String scope) {
        return new DefaultSettingsStatusProvider().getStatus(ConfigurationStatusProvider.Context.builder().scope(scope).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/document-properties-pane-config")
    @Operation(
            summary = "Checks document properties pane configuration",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved document properties pane configuration",
                            content = @Content(schema = @Schema(implementation = ConfigurationStatus.class))
                    )
            }
    )
    public @NotNull ConfigurationStatus checkDocumentPropertiesPaneConfig(@QueryParam("scope") @DefaultValue("") String scope) {
        return new DocumentPropertiesPaneStatusProvider().getStatus(ConfigurationStatusProvider.Context.builder().scope(scope).build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/dle-toolbar-config")
    @Operation(
            summary = "Checks DLE Toolbar configuration",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved DLE Toolbar configuration",
                            content = @Content(schema = @Schema(implementation = ConfigurationStatus.class))
                    )
            }
    )
    public @NotNull ConfigurationStatus checkDleToolbarConfig() {
        return new DleToolbarStatusProvider().getStatus(ConfigurationStatusProvider.Context.builder().build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/live-report-config")
    @Operation(
            summary = "Checks Live Report configuration",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved Live Report configuration",
                            content = @Content(schema = @Schema(implementation = ConfigurationStatus.class))
                    )
            }
    )
    public @NotNull ConfigurationStatus checkLiveReportConfig() {
        return new LiveReportMainHeadStatusProvider().getStatus(ConfigurationStatusProvider.Context.builder().build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/cors-config")
    @Operation(
            summary = "Checks CORS configuration",
            description = "Retrieves the status of the CORS configuration.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved CORS configuration",
                            content = @Content(schema = @Schema(implementation = ConfigurationStatus.class))
                    )
            }
    )
    public @NotNull ConfigurationStatus checkCORSConfig() {
        return new CORSStatusProvider().getStatus(ConfigurationStatusProvider.Context.builder().build());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/configuration/weasyprint")
    @Operation(
            summary = "Checks WeasyPrint configuration",
            description = "Retrieves the status of the WeasyPrint configuration.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved WeasyPrint configuration",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConfigurationStatus.class)))
                    )
            }
    )
    public @NotNull List<ConfigurationStatus> checkWeasyPrint() {
        return new WeasyPrintStatusProvider().getStatuses(ConfigurationStatusProvider.Context.builder().build());
    }
}
