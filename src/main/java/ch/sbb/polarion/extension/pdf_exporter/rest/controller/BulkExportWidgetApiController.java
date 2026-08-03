package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItems;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets.BulkExportItemsRequest;
import ch.sbb.polarion.extension.pdf_exporter.util.BulkExportWidgetHelper;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

@Singleton
@Secured
@Path("/api")
public class BulkExportWidgetApiController extends BulkExportWidgetInternalController {

    public BulkExportWidgetApiController() {
        super();
    }

    public BulkExportWidgetApiController(BulkExportWidgetHelper bulkExportWidgetHelper) {
        super(bulkExportWidgetHelper);
    }

    /**
     * Not privileged, unlike the other api controllers: the rows a widget shows depend on what the calling user may
     * read, and elevating that would hand out items the user cannot see on the page itself.
     */
    @Override
    public BulkExportItems getItems(BulkExportItemsRequest request) {
        return super.getItems(request);
    }
}
