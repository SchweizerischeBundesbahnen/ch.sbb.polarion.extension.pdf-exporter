package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.projects.model.IUniqueObject;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Builder
@Getter
public class DocumentData<T extends IUniqueObject> {
    private final @Nullable String projectName;
    private final @NotNull DocumentType documentType;
    private final @NotNull T documentObject;
    private String lastRevision;
    private String baselineName;
    private String documentId;
    private String documentTitle;
    private String documentContent;
}
