package ch.sbb.polarion.extension.pdf_exporter.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Callback class containing metadata information related to export")
public class ExportMetaInfoCallback {

    @Schema(description = "List of work items linked to the export process",
            implementation = WorkItemRefData.class)
    private List<WorkItemRefData> linkedWorkItems;
}

