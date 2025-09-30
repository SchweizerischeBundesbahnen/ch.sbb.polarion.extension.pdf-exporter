package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocIdentifierTest {

    @Test
    void testDocIdentifierFromExportParams() {
        ExportParams exportParams = ExportParams.builder().projectId("projectId").locationPath("space/doc").build();
        DocIdentifier docIdentifier = DocIdentifier.of(exportParams);
        assertEquals("projectId", docIdentifier.getProjectId());
        assertEquals("space", docIdentifier.getSpaceId());
        assertEquals("doc", docIdentifier.getDocumentName());
    }
}
