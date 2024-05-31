package ch.sbb.polarion.extension.pdf.exporter.rest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NestedListsCheck {
    private boolean containsNestedLists;
}
