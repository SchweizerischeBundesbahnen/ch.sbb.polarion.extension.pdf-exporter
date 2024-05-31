package ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public enum Orientation {
    PORTRAIT,
    LANDSCAPE;

    public String toCssString() {
        return toString().toLowerCase();
    }

    @JsonCreator
    public static Orientation fromString(String name) {
        try {
            return (name != null) ? valueOf(name.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            // Necessary to return correct HTTP error code by query parameters conversion
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Unsupported value for orientation parameter: " + name)
                    .build());
        }
    }
}
