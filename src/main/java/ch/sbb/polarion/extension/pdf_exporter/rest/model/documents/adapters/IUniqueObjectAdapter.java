package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentBaseline;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IUniqueObjectAdapter {

    @NotNull <T extends IUniqueObject> T getUniqueObject();

    @NotNull DocumentId getDocumentId();

    @NotNull DocumentType getDocumentType();

    @NotNull String getTitle();

    @Nullable String getRevision();

    @NotNull String getLastRevision();

    @NotNull String getContent(@NotNull ExportParams exportParams);

    @NotNull String getContent(@NotNull ExportParams exportParams, @NotNull ReadOnlyTransaction transaction);

    @NotNull DocumentBaseline getDocumentBaseline();

    @NotNull DocumentBaseline getDocumentBaseline(@NotNull ReadOnlyTransaction transaction);
}
