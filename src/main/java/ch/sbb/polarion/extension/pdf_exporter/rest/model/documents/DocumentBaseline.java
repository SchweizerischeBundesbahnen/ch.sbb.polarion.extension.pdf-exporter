package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import com.polarion.alm.tracker.model.IBaseline;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocumentBaseline {
    private final @Nullable String projectBaselineName;
    private final @Nullable String documentBaselineName;

    public DocumentBaseline(@Nullable String projectBaselineName, @Nullable String documentBaselineName) {
        this.projectBaselineName = projectBaselineName;
        this.documentBaselineName = documentBaselineName;
    }

    public String asPlaceholder() {
        List<String> names = new ArrayList<>();

        if (projectBaselineName != null) {
            names.add("pb. " + projectBaselineName);
        }

        if (documentBaselineName != null) {
            names.add("db. " + documentBaselineName);
        }

        return String.join(" | ", names);
    }

    public static @NotNull DocumentBaseline from(@Nullable IBaseline projectBaseline, @Nullable IBaseline moduleBaseline) {
        String projectBaselineName = projectBaseline != null ? projectBaseline.getName() : null;
        String documentBaselineName = moduleBaseline != null ? moduleBaseline.getName() : null;
        return new DocumentBaseline(projectBaselineName, documentBaselineName);
    }

    public static @NotNull DocumentBaseline empty() {
        return new DocumentBaseline(null, null);
    }

}
