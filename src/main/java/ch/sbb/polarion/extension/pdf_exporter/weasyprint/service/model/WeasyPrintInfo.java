package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeasyPrintInfo {
    private @Nullable Integer apiVersion;
    private @Nullable String chromium;
    private @Nullable String python;
    private @Nullable String timestamp;
    private @Nullable String weasyprint;
    private @Nullable String weasyprintService;
}
