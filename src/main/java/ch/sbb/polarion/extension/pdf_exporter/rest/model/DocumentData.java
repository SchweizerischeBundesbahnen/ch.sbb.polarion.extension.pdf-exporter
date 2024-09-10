package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import com.polarion.alm.projects.model.IUniqueObject;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Builder(builderMethodName = "create")
@Getter
public class DocumentData<T extends IUniqueObject> {
    private final @NotNull DocumentType documentType;
    private final @NotNull T documentObject;

    private @NotNull String id;
    private @NotNull String title;
    private @Nullable String content;

    private @Nullable String projectName;
    private @Nullable String lastRevision;
    private @Nullable String baselineName;

    public static <T extends IUniqueObject> DocumentDataBuilder<T> builder(@NotNull DocumentType documentType, @NotNull T documentObject) {
        return DocumentData.<T>create()
                .documentType(documentType)
                .documentObject(documentObject);
    }

    // making javadoc maven plugin happy
    public static class DocumentDataBuilder<T extends IUniqueObject> {}
}
