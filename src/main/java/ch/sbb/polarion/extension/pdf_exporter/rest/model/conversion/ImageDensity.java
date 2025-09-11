package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Schema(description = "Quality of PNG images converted from SVG")
@Getter
public enum ImageDensity {
    DPI_96(1.0),
    DPI_192(2.0),
    DPI_300(3.125),
    DPI_600(6.25);

    private final double scale;

    ImageDensity(double scale) {
        this.scale = scale;
    }

    @JsonCreator
    public static ImageDensity fromString(String name) {
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
}
