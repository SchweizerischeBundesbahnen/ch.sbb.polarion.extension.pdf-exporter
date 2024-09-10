package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import lombok.Builder;

@Builder
public record WeasyPrintOptions(boolean followHTMLPresentationalHints) {
    public WeasyPrintOptions() {
        this(false);
    }
}
