package ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of the document")
public enum DocumentType {
    @Schema(description = "Live document")
    DOCUMENT,

    @Schema(description = "Wiki page")
    WIKI,

    @Schema(description = "Live report")
    REPORT;

    @SuppressWarnings("unused")
    @JsonCreator
    public static DocumentType fromString(String name) {
        return valueOf(name.toUpperCase());
    }
}
