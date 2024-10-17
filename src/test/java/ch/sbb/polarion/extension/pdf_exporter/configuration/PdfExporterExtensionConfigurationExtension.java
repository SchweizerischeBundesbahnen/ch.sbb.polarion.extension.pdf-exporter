package ch.sbb.polarion.extension.pdf_exporter.configuration;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.mockStatic;

public class PdfExporterExtensionConfigurationExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedStatic<PdfExporterExtensionConfiguration> pdfExporterExtensionConfigurationMockedStatic;

    private static PdfExporterExtensionConfiguration pdfExporterExtensionConfiguration;

    public static void setPdfExporterExtensionConfigurationMock(PdfExporterExtensionConfiguration mock) {
        pdfExporterExtensionConfiguration = mock;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (pdfExporterExtensionConfiguration == null) {
            pdfExporterExtensionConfiguration = Mockito.mock(PdfExporterExtensionConfiguration.class);
        }

        pdfExporterExtensionConfigurationMockedStatic = mockStatic(PdfExporterExtensionConfiguration.class);
        pdfExporterExtensionConfigurationMockedStatic.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(pdfExporterExtensionConfiguration);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (pdfExporterExtensionConfigurationMockedStatic != null) {
            pdfExporterExtensionConfigurationMockedStatic.close();
        }
        pdfExporterExtensionConfiguration = null;
    }
}
