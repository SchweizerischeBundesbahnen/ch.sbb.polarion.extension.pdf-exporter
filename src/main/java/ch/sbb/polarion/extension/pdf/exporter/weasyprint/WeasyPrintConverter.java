package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import java.lang.module.ModuleDescriptor;

public interface WeasyPrintConverter {
    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions);

    ModuleDescriptor.Version getWeasyPrintVersion();
}
