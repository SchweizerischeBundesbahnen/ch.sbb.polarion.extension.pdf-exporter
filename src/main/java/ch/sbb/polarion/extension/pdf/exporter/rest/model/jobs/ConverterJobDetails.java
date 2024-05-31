package ch.sbb.polarion.extension.pdf.exporter.rest.model.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConverterJobDetails {
    private ConverterJobStatus status;
    private String errorMessage;
}
