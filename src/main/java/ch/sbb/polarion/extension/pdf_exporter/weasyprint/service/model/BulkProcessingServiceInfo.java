package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class BulkProcessingServiceInfo {
    private @Nullable Integer apiVersion;
    private @Nullable String python;
    private @Nullable String bulkProcessingService;
    private @Nullable String timestamp;
}
