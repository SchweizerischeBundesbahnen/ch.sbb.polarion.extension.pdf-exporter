package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsCleaner;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, PdfExporterExtensionConfigurationExtension.class, PlatformContextMockExtension.class})
class ExtensionBundleActivatorTest {

    @AfterEach
    void tearDown() {
        PdfConverterJobsCleaner.stopCleaningJob();
    }

    @Test
    void testBundleActivator() {
        assertEquals("pdf-exporter", new ExtensionBundleActivator().getExtensions().keySet().iterator().next());
    }

    @Test
    void testNamedSettingsRegistration() {
        ExtensionBundleActivator bundleActivator = new ExtensionBundleActivator();
        BundleContext bundleContext = mock(BundleContext.class);

        assertDoesNotThrow(() -> bundleActivator.start(bundleContext));
    }

    @Test
    void testSettingsRegistrationExceptionIsCaught() {
        try (MockedConstruction<CssSettings> ignored = mockConstruction(CssSettings.class, (mock, context) -> {
            throw new RuntimeException("Simulated CssSettings creation failure");
        })) {
            ExtensionBundleActivator bundleActivator = new ExtensionBundleActivator();
            BundleContext bundleContext = mock(BundleContext.class);

            assertDoesNotThrow(() -> bundleActivator.start(bundleContext));
        }
    }

    @Test
    void testCleaningJobExceptionIsCaught() {
        try (MockedStatic<PdfConverterJobsCleaner> mockedCleaner = mockStatic(PdfConverterJobsCleaner.class)) {
            mockedCleaner.when(PdfConverterJobsCleaner::startCleaningJob)
                    .thenThrow(new RuntimeException("Simulated cleaning job failure"));

            ExtensionBundleActivator bundleActivator = new ExtensionBundleActivator();
            BundleContext bundleContext = mock(BundleContext.class);

            assertDoesNotThrow(() -> bundleActivator.start(bundleContext));
        }
    }

}
