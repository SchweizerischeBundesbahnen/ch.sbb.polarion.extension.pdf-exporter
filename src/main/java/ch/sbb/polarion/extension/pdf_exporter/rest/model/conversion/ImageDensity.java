package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Deprecated: weasyprint-service draws SVG as vector since 70.0.8 and ignores the image density.
 * The next major version removes it. The OpenAPI schema marks it, not {@code @Deprecated}: the REST
 * clients are the ones to act, and the extension itself keeps using it until then.
 */
@Schema(description = "Quality of PNG images converted from SVG. Deprecated: weasyprint-service 70.0.8 and later draws SVG as vector and ignores it",
        deprecated = true)
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
