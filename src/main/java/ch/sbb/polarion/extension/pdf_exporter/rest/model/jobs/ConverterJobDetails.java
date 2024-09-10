package ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of the converter job including status and error message if any")
public class ConverterJobDetails {

    @Schema(description = "Current status of the converter job",
            example = "IN_PROGRESS",
            implementation = ConverterJobStatus.class
    )
    private ConverterJobStatus status;

    @Schema(description = "Error message if the conversion failed")
    private String errorMessage;
}
