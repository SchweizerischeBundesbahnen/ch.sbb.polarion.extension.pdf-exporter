package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.MergeJobStartParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector.MergeDocumentData;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** A service which cannot be reached, where the merge fails instead of waiting. */
class UnreachableServiceTest extends BaseBulkProcessingTest {

    @Test
    void failsWhereTheServiceCannotBeReached() {
        BulkProcessingServiceConnector unreachable = connector("http://localhost:1");
        List<MergeDocumentData> documents = List.of(rendered("Alpha", "Never rendered."));
        MergeJobStartParams params = startParams();

        assertThrows(ProcessingException.class, () -> unreachable.convertMergedToPdf(documents, params));
    }
}
