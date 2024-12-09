package ch.sbb.polarion.extension.pdf_exporter.rest.controller;


import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.DocumentCollectionEntry;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Hidden
@Path("/internal")
@Tag(name = "Collections")
public class CollectionInternalController {
    private final PdfExporterPolarionService pdfExporterPolarionService;

    public CollectionInternalController() {
        pdfExporterPolarionService = new PdfExporterPolarionService();
    }

    @GET
    @Path("/projects/{projectId}/collections/{collectionId}/documents")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get documents from collection",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Successfully retrieved the list of documents in the collection",
                            content = @Content(schema = @Schema(implementation = DocumentCollectionEntry.class))
                    )
            }
    )
    public List<DocumentCollectionEntry> getDocumentsFromCollection(
            @Parameter(description = "Project ID", required = true) @PathParam("projectId") String projectId,
            @Parameter(description = "Collection ID", required = true) @PathParam("collectionId") String collectionId,
            @Parameter(description = "The specific revision of the provided collection") @QueryParam("revision") String revision
    ) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> pdfExporterPolarionService.getDocumentsFromCollection(projectId, collectionId, revision, transaction));
    }
}
