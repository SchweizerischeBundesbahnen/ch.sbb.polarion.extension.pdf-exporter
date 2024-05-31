package ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration;

public enum Status {
    OK,
    WARNING,
    ERROR;

    public String toHtml() {
        String color = switch (this) {
            case OK -> "green";
            case WARNING -> "gray";
            case ERROR -> "red";
        };
        return String.format("<span style='color: %s;'>%s</span>", color, this.name());
    }
}
