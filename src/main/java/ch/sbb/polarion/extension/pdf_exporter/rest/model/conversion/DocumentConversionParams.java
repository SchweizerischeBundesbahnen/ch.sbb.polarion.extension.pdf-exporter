package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Per-document conversion parameters for WeasyPrint")
public class DocumentConversionParams {

    @Schema(description = "Character encoding for the HTML document", defaultValue = "utf-8")
    @Builder.Default
    private String encoding = "utf-8";

    @Schema(description = "CSS media type to use during rendering", defaultValue = "print")
    @Builder.Default
    private String mediaType = "print";

    @Schema(description = "Whether to follow HTML presentational hints", defaultValue = "false")
    @Builder.Default
    private boolean presentationalHints = false;

    @Schema(description = "Base URL for resolving relative URLs in the HTML document")
    private String baseUrl;

    @Schema(description = "Scale factor for images")
    private String scaleFactor;

    @Schema(description = "PDF variant to be used for conversion (e.g. pdf/a-2b)")
    private String pdfVariant;

    @Schema(description = "Whether to include custom metadata in the PDF", defaultValue = "false")
    @Builder.Default
    private boolean customMetadata = false;

    @Schema(description = "Embed full fonts instead of subsetting", defaultValue = "false")
    @Builder.Default
    private boolean fullFonts = false;
}
