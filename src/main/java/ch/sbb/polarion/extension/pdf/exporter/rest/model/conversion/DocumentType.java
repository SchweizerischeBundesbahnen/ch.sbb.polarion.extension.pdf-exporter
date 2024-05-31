package ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentType {
    DOCUMENT,
    WIKI,
    REPORT;

    @SuppressWarnings("unused")
    @JsonCreator
    public static DocumentType fromString(String name) {
        return valueOf(name.toUpperCase());
    }
}
