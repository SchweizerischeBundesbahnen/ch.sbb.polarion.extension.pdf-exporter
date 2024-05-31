package ch.sbb.polarion.extension.pdf.exporter.weasyprint.exporter;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.properties.WeasyPrintConnector;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConnectorFactory;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.mockito.MockedStatic;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import static org.mockito.Mockito.*;

public class WeasyPrintExporterCommandImpl implements WeasyPrintExporter {

    public static final String COMMAND_PARAM = "wpCommand";

    @Override
    @SneakyThrows
    @SuppressWarnings({"nullable"})
    public byte[] exportToPdf(@NotNull String html, @NotNull WeasyPrintOptions weasyPrintOptions) {
        //use mockStatic to override PdfExporterExtensionConfiguration.getInstance().getWeasyPrintExecutable() call in WeasyPrintExecutor.
        try (MockedStatic<PdfExporterExtensionConfiguration> staticConfiguration = mockStatic(PdfExporterExtensionConfiguration.class)) {
            PdfExporterExtensionConfiguration configuration = mock(PdfExporterExtensionConfiguration.class);
            staticConfiguration.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);
            String commandString = StringUtils.defaultString(System.getProperty(COMMAND_PARAM), PdfExporterExtensionConfiguration.WEASYPRINT_EXECUTABLE_DEFAULT);
            when(configuration.getWeasyprintExecutable()).thenReturn(commandString);

            return WeasyPrintConnectorFactory.getWeasyPrintExecutor(WeasyPrintConnector.CLI).convertToPdf(html, weasyPrintOptions);
        }
    }
}
