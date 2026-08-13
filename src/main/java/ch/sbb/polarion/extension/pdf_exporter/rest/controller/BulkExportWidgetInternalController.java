package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItemsRequest;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportWidgetDescriptor;
import ch.sbb.polarion.extension.pdf_exporter.util.BulkExportWidgetHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.WidgetDescriptorSigner;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Singleton
@Hidden
@Path("/internal")
@Tag(name = "Widgets")
public class BulkExportWidgetInternalController {

    private final BulkExportWidgetHelper bulkExportWidgetHelper;

    public BulkExportWidgetInternalController() {
        this(new BulkExportWidgetHelper());
    }

    public BulkExportWidgetInternalController(BulkExportWidgetHelper bulkExportWidgetHelper) {
        this.bulkExportWidgetHelper = bulkExportWidgetHelper;
    }

    @POST
    @Path("/widgets/bulk-export/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the rows of a bulk export widget",
            description = "Executes the data set the widget resolved when it was rendered. The descriptor must carry "
                    + "the signature the renderer issued for it, otherwise the request is rejected.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved the rows of the widget",
                            content = @Content(schema = @Schema(implementation = BulkExportItems.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "The descriptor is missing, malformed or not signed by this server")
            }
    )
    public BulkExportItems getItems(BulkExportItemsRequest request) {
        if (request == null || !WidgetDescriptorSigner.getInstance().verify(request.getDescriptor(), request.getSignature())) {
            // Also the answer after a server restart, which invalidates the signatures of pages that are still open.
            throw new BadRequestException("The widget descriptor is missing or was not signed by this server. Reload the page.");
        }
        BulkExportWidgetDescriptor descriptor = WidgetDescriptorSigner.getInstance().decode(request.getDescriptor(), BulkExportWidgetDescriptor.class);
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> bulkExportWidgetHelper.getItems(descriptor, transaction));
    }
}
