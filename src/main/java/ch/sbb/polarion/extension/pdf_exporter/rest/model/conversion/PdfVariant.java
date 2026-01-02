package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * PDF Variants
 */
@Schema(description = "PDF variants")
public enum PdfVariant {
    PDF_A_1A,
    PDF_A_1B,
    PDF_A_2A,
    PDF_A_2B,
    PDF_A_2U,
    PDF_A_3A,
    PDF_A_3B,
    PDF_A_3U,
    PDF_A_4E,
    PDF_A_4F,
    PDF_A_4U,
    PDF_UA_1,
    PDF_UA_2;

    @JsonCreator
    public static PdfVariant fromString(String name) {
        try {
            return (name != null) ? valueOf(name.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            // Necessary to return correct HTTP error code by query parameters conversion
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Unsupported value for pdfVariant parameter: " + name)
                    .build());
        }
    }

    public String toWeasyPrintParameter() {
        return switch (this) {
            case PDF_A_1A -> "pdf/a-1a";
            case PDF_A_1B -> "pdf/a-1b";
            case PDF_A_2A -> "pdf/a-2a";
            case PDF_A_2B -> "pdf/a-2b";
            case PDF_A_2U -> "pdf/a-2u";
            case PDF_A_3A -> "pdf/a-3a";
            case PDF_A_3B -> "pdf/a-3b";
            case PDF_A_3U -> "pdf/a-3u";
            case PDF_A_4E -> "pdf/a-4e";
            case PDF_A_4F -> "pdf/a-4f";
            case PDF_A_4U -> "pdf/a-4u";
            case PDF_UA_1 -> "pdf/ua-1";
            case PDF_UA_2 -> "pdf/ua-2";
        };
    }

}
