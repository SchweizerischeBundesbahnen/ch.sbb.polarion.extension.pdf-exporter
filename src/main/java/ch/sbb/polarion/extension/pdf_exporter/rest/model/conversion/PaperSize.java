package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Enumeration of paper sizes <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/@page/size">supported by CSS</a>
 */
@Schema(description = "Standard paper sizes")
public enum PaperSize {
    A5,
    A4,
    A3,
    B5,
    B4,
    JIS_B5,
    JIS_B4,
    LETTER,
    LEGAL,
    LEDGER;

    @JsonCreator
    public static PaperSize fromString(String name) {
        try {
            return (name != null) ? valueOf(name.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            // Necessary to return correct HTTP error code by query parameters conversion
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Unsupported value for paperSize parameter: " + name)
                    .build());
        }
    }

    public String toCssString() {
        return switch (this) {
            case JIS_B5 -> "JIS-B5";
            case JIS_B4 -> "JIS-B4";
            case LETTER, LEGAL, LEDGER -> toString().toLowerCase();
            default -> toString();
        };
    }
}
