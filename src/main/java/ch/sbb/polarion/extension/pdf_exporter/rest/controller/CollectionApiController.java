package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.CollectionItem;

import javax.ws.rs.Path;
import java.util.List;

@Secured
@Path("/api")
public class CollectionApiController extends CollectionInternalController {
    private static final PolarionService polarionService = new PolarionService();

    @Override
    public List<CollectionItem> getCollectionItems(String projectId, String collectionId) {
        return polarionService.callPrivileged(() -> super.getCollectionItems(projectId, collectionId));
    }
}
