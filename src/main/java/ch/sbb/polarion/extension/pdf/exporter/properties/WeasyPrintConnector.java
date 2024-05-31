package ch.sbb.polarion.extension.pdf.exporter.properties;

public enum WeasyPrintConnector {
    SERVICE,
    CLI;

    @SuppressWarnings("unused")
    public static WeasyPrintConnector fromString(String name) {
        return valueOf(name.toUpperCase());
    }
}

