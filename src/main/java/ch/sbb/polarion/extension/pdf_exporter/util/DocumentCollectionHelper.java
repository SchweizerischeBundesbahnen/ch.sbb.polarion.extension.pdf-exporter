package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.DocumentCollectionEntry;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DocumentCollectionHelper {
    private final DocumentFileNameHelper documentFileNameHelper;

    public DocumentCollectionHelper() {
        this.documentFileNameHelper = new DocumentFileNameHelper();
    }

    public DocumentCollectionHelper(DocumentFileNameHelper documentFileNameHelper) {
        this.documentFileNameHelper = documentFileNameHelper;
    }

    public @NotNull List<DocumentCollectionEntry> getDocumentsFromCollection(
            @NotNull String projectId,
            @NotNull String collectionId,
            @Nullable String revision,
            @NotNull ReadOnlyTransaction transaction
    ) {
        List<DocumentCollectionEntry> result = new ArrayList<>();

        BaselineCollectionReference baselineCollectionReference = new BaselineCollectionReference(projectId, collectionId);
        if (revision != null) {
            baselineCollectionReference = baselineCollectionReference.getWithRevision(revision);
        }

        BaselineCollection baselineCollection = baselineCollectionReference.get(transaction);
        IBaselineCollection collection = baselineCollection.getOldApi();

        List<IModule> modules =
                Stream.concat(
                                collection.getElements().stream(),
                                collection.getUpstreamCollections().stream()
                                        .flatMap(upstream -> upstream.getElements().stream())
                        )
                        .map(IBaselineCollectionElement::getObjectWithRevision)
                        .filter(IModule.class::isInstance)
                        .map(IModule.class::cast)
                        .toList();

        for (IModule module : modules) {
            DocumentCollectionEntry documentCollectionEntry = new DocumentCollectionEntry(
                    module.getProjectId(),
                    module.getModuleFolder(),
                    module.getModuleName(),
                    module.getRevision(),
                    documentFileNameHelper.getDocumentFileName(
                            ExportParams.builder()
                                    .projectId(projectId)
                                    .documentType(DocumentType.LIVE_DOC)
                                    .locationPath(module.getModuleLocation().getLocationPath())
                                    .revision(module.getRevision())
                                    .build()
                    )
            );
            result.add(documentCollectionEntry);
        }

        return result;
    }
}
