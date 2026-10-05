package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The version of the service, which the configuration status of the extension reports. */
class ServiceVersionTest extends BaseBulkProcessingTest {

    @Test
    void readsTheVersionOfTheService() {
        assertNotNull(connector().getVersionInfo().getBulkProcessingService());
    }
}
