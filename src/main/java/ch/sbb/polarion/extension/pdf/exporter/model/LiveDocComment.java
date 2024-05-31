package ch.sbb.polarion.extension.pdf.exporter.model;

import com.polarion.alm.shared.api.model.fields.BooleanField;
import com.polarion.alm.shared.api.model.fields.DateField;
import com.polarion.alm.shared.api.model.fields.IdField;
import com.polarion.alm.shared.api.model.fields.RichTextField;
import com.polarion.alm.shared.api.model.fields.StringField;
import com.polarion.alm.shared.api.model.project.ProjectField;
import com.polarion.alm.shared.api.model.user.UserField;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LiveDocComment {
    private IdField id;
    private StringField title;
    private RichTextField text;
    private ProjectField project;
    private UserField author;
    private DateField created;
    private BooleanField resolved;
    private Map<String, LiveDocComment> childComments;
}