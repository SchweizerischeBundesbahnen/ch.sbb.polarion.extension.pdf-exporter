package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Schema(description = "Direction of linked workitem roles resolution")
public enum LinkRoleDirection {
    @Schema(description = "Both direct and reverse roles")
    BOTH,

    @Schema(description = "Direct roles only")
    DIRECT,

    @Schema(description = "Reverse roles only")
    REVERSE;

    @JsonCreator
    public static LinkRoleDirection fromString(String name) {
        try {
            return (name != null) ? valueOf(name.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Unsupported value for linkRoleDirection parameter: " + name)
                    .build());
        }
    }
}
