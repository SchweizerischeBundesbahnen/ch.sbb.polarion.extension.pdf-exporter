package ch.sbb.polarion.extension.pdf_exporter.rest;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, PdfExporterExtensionConfigurationExtension.class, PlatformContextMockExtension.class})
class PdfExporterRestApplicationTest {

    @Test
    @SuppressWarnings("unused")
    void testInitialization() {
        try (MockedConstruction<PdfExporterFileResourceProvider> resourceProviderMock = mockConstruction(PdfExporterFileResourceProvider.class)) {
            PdfExporterRestApplication application = new PdfExporterRestApplication();
            assertDoesNotThrow(application::getExtensionControllerSingletons);
            assertDoesNotThrow(application::getExtensionExceptionMapperSingletons);
            assertDoesNotThrow(application::getExtensionFilterSingletons);
        }
    }

}
