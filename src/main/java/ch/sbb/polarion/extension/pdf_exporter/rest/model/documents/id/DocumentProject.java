package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id;

import com.polarion.alm.projects.model.IProject;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class DocumentProject {
    private final String id;
    private final String name;

    public DocumentProject(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public DocumentProject(@NotNull IProject project) {
        this(project.getId(), project.getName());
    }
}
