package ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class WeasyPrintInfo {
    private @Nullable String python;
    private @Nullable String weasyprint;
    private @Nullable String weasyprintService;
}
