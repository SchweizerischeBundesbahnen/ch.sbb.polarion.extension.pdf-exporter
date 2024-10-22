package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeasyPrintInfo {
    private @Nullable String chromium;
    private @Nullable String python;
    private @Nullable String timestamp;
    private @Nullable String weasyprint;
    private @Nullable String weasyprintService;
}
