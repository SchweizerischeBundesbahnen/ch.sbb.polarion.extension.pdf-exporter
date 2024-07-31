package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.model.WeasyPrintInfo;

public interface WeasyPrintConverter {
    byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions);

    WeasyPrintInfo getWeasyPrintInfo();
}
