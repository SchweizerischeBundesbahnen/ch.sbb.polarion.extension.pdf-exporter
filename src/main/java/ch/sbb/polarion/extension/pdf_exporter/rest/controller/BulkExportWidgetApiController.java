package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.pdf_exporter.util.BulkExportWidgetHelper;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

/**
 * The public face of the widget endpoint.
 * <p>
 * The endpoint is inherited unchanged on purpose. The other api controllers wrap their inherited method in
 * {@code callPrivileged}, which this one must not do: the rows a widget shows depend on what the calling user may
 * read, and elevating that would hand out items the user cannot see on the page itself.
 */
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
}
