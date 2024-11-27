package ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments;

import com.polarion.alm.tracker.model.ITestRunAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunAttachment {
    private String id;
    private String fileName;
    private String revision;

    public static @NotNull TestRunAttachment fromAttachment(@NotNull ITestRunAttachment attachment) {
        return TestRunAttachment.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .revision(attachment.getRevision())
                .build();
    }
}
