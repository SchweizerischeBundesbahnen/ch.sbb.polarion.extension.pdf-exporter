package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of the document")
public enum DocumentType {
    @Schema(description = "Baseline collection")
    BASELINE_COLLECTION,

    @Schema(description = "Live document")
    LIVE_DOC,

    @Schema(description = "Live report")
    LIVE_REPORT,

    @Schema(description = "Test run")
    TEST_RUN,

    @Schema(description = "Wiki page")
    WIKI_PAGE;

    @SuppressWarnings("unused")
    @JsonCreator
    public static DocumentType fromString(String name) {
        return valueOf(name.toUpperCase());
    }
}
