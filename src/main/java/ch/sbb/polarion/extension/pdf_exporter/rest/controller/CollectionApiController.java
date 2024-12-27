package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.DocumentCollectionEntry;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.google.inject.Inject;

import javax.ws.rs.Path;
import java.util.List;

@Secured
@Path("/api")
public class CollectionApiController extends CollectionInternalController {
    private static final PolarionService polarionService = new PolarionService();

    @Inject
    public CollectionApiController(PdfExporterPolarionService pdfExporterPolarionService) {
        super(pdfExporterPolarionService);
    }

    @Override
    public List<DocumentCollectionEntry> getDocumentsFromCollection(String projectId, String collectionId, String revision) {
        return polarionService.callPrivileged(() -> super.getDocumentsFromCollection(projectId, collectionId, revision));
    }
}
