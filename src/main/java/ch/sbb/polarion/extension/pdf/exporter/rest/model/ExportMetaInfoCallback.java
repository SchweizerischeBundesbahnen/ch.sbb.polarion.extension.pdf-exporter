package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import lombok.Data;

import java.util.List;

@Data
public class ExportMetaInfoCallback {
    private List<WorkItemRefData> linkedWorkItems;
}
