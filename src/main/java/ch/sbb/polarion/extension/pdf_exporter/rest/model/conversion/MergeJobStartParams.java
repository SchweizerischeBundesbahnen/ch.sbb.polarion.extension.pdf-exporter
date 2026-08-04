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
@Schema(description = "Job-level parameters for starting a merge PDF conversion job")
public class MergeJobStartParams {

    @Schema(description = "Output file name for the merged PDF", defaultValue = "merged-document.pdf")
    @Builder.Default
    private String fileName = "merged-document.pdf";

    @Schema(description = "PDF variant for post-processing (e.g. pdf/a-2b)")
    private String pdfVariant;
}
