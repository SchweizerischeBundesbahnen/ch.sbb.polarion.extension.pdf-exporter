package ch.sbb.polarion.extension.pdf.exporter.rest.model.jobs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of the converter job")
public enum ConverterJobStatus {

    @Schema(description = "The conversion is currently in progress")
    IN_PROGRESS,

    @Schema(description = "The conversion has finished successfully")
    SUCCESSFULLY_FINISHED,

    @Schema(description = "The conversion has failed")
    FAILED,

    @Schema(description = "The conversion was cancelled")
    CANCELLED
}