package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import com.polarion.alm.tracker.model.IBaseline;
import lombok.Data;
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

    public DocumentBaseline(@Nullable IBaseline projectBaseline, @Nullable IBaseline moduleBaseline) {
        this.projectBaselineName = projectBaseline != null ? projectBaseline.getName() : null;
        this.documentBaselineName = moduleBaseline != null ? moduleBaseline.getName() : null;
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
}
