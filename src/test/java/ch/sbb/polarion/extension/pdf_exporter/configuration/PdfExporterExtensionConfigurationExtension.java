package ch.sbb.polarion.extension.pdf_exporter.configuration;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Mocks {@link PdfExporterExtensionConfiguration#getInstance()} for each test. The mock belongs to the thread of the
 * test, so that test classes may run in parallel: a class's tests run in one thread, from before to after each.
 */
public class PdfExporterExtensionConfigurationExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ThreadLocal<PdfExporterExtensionConfiguration> CONFIGURATION = new ThreadLocal<>();
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PdfExporterExtensionConfigurationExtension.class);

    public static void setPdfExporterExtensionConfigurationMock(PdfExporterExtensionConfiguration mock) {
        CONFIGURATION.set(mock);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (CONFIGURATION.get() == null) {
            CONFIGURATION.set(mock(PdfExporterExtensionConfiguration.class));
        }
        PdfExporterExtensionConfiguration configuration = CONFIGURATION.get();
        MockedStatic<PdfExporterExtensionConfiguration> mockedStatic = mockStatic(PdfExporterExtensionConfiguration.class);
        mockedStatic.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);
        extensionContext.getStore(NAMESPACE).put(MockedStatic.class, mockedStatic);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        MockedStatic<?> mockedStatic = extensionContext.getStore(NAMESPACE).remove(MockedStatic.class, MockedStatic.class);
        if (mockedStatic != null) {
            mockedStatic.close();
        }
        CONFIGURATION.remove();
    }
}
