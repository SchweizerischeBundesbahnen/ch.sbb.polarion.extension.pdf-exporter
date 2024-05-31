package ch.sbb.polarion.extension.pdf.exporter.weasyprint.exporter;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface WeasyPrintExporter {

    Map<String, WeasyPrintExporter> IMPL_REGISTRY = Map.of(
            "docker", new WeasyPrintExporterDockerImpl(),
            "command", new WeasyPrintExporterCommandImpl()
    );

    byte[] exportToPdf(@NotNull String html, @NotNull WeasyPrintOptions weasyPrintOptions);
}
