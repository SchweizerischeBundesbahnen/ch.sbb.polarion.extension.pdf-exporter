package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Parameters for bulk merge export: multiple documents merged into a single PDF")
public class BulkMergeExportParams {

    @Schema(description = "List of export parameters, one per document to be merged")
    private List<ExportParams> documents;

    @Schema(description = "Parameters for the merge session")
    private MergeSessionStartParams mergeSessionParams;
}
