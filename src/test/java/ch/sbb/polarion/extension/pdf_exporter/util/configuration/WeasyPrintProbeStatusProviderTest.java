package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.ProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class WeasyPrintProbeStatusProviderTest {

    @Test
    void testHappyPath() {
        HtmlToPdfConverter htmlToPdfConverter = mock(HtmlToPdfConverter.class);
        when(htmlToPdfConverter.convert("<html><body>test html</body></html>", ConversionParams.builder().build())).thenReturn(new byte[0]);
        WeasyPrintProbeStatusProvider weasyPrintProbeStatusProvider = new WeasyPrintProbeStatusProvider(htmlToPdfConverter);

        ConfigurationStatus status = weasyPrintProbeStatusProvider.getStatus(ConfigurationStatusProvider.Context.builder().build());

        assertEquals(new ConfigurationStatus("WeasyPrint Service: WeasyPrint probe", Status.OK), status);
    }

    @Test
    void testConnectionRefused() {
        HtmlToPdfConverter htmlToPdfConverter = mock(HtmlToPdfConverter.class);
        when(htmlToPdfConverter.convert("<html><body>test html</body></html>", ConversionParams.builder().build())).thenThrow(new ProcessingException("java.net.ConnectException: Connection refused"));
        WeasyPrintProbeStatusProvider weasyPrintProbeStatusProvider = new WeasyPrintProbeStatusProvider(htmlToPdfConverter);

        ConfigurationStatus status = weasyPrintProbeStatusProvider.getStatus(ConfigurationStatusProvider.Context.builder().build());

        assertEquals(new ConfigurationStatus("WeasyPrint Service: WeasyPrint probe", Status.ERROR, "java.net.ConnectException: Connection refused"), status);
    }
}
