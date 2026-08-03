package ch.sbb.polarion.extension.pdf_exporter.rest.model.widgets;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The data set of a Bulk PDF Export widget, resolved when the widget was rendered on the page.
 * <p>
 * The widget's parameters (scope, query type, query, sorting) only exist in the widget rendering context, so the
 * renderer resolves them once and hands the result to the browser, which passes it back with every request for the
 * widget's rows. The browser never sees this object as JSON: it travels base64-encoded and signed, and
 * {@link ch.sbb.polarion.extension.pdf_exporter.util.WidgetDescriptorSigner} rejects anything it did not produce
 * itself. Without that check the endpoint would execute any query - including arbitrary SQL - that a reader of the
 * page cares to send.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "The resolved data set of a bulk export widget")
public class BulkExportWidgetDescriptor {

    @Schema(description = "Name of the Polarion prototype the widget lists", example = "TestRun")
    private String prototype;

    @Schema(description = "Type of document the listed items are exported as")
    private DocumentType documentType;

    @Schema(description = "The resolved query: Lucene with scope and subtype already applied, or SQL")
    private String query;

    @Schema(description = "Whether the query is SQL. Lucene otherwise")
    private boolean sqlQuery;

    @Schema(description = "Lucene sort string built from the widget's sorting parameter")
    private String sort;

    @Schema(description = "Maximum number of items to list", example = "50")
    private int top;

    @Schema(description = "Id of the project the widget is scoped to, null in the repository scope")
    private String projectId;

    @Schema(description = "Revision the items are read at, null for the current one")
    private String revision;

    @Schema(description = "Id of the project of the baseline collection the page is opened in")
    private String collectionProjectId;

    @Schema(description = "Id of the baseline collection the page is opened in")
    private String collectionId;

    @Schema(description = "Columns to render for every item")
    private List<BulkExportColumn> columns;
}
