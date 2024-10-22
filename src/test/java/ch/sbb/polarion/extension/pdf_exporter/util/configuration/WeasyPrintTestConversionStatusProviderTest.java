package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
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
class WeasyPrintTestConversionStatusProviderTest {

    @Test
    void testHappyPath() {
        HtmlToPdfConverter htmlToPdfConverter = mock(HtmlToPdfConverter.class);
        when(htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4)).thenReturn(new byte[0]);
        WeasyPrintTestConversionStatusProvider weasyPrintTestConversionStatusProvider = new WeasyPrintTestConversionStatusProvider(htmlToPdfConverter);

        ConfigurationStatus status = weasyPrintTestConversionStatusProvider.getStatus(ConfigurationStatusProvider.Context.builder().build());

        assertEquals(new ConfigurationStatus("WeasyPrint Service: Test conversion", Status.OK), status);
    }

    @Test
    void testConnectionRefused() {
        HtmlToPdfConverter htmlToPdfConverter = mock(HtmlToPdfConverter.class);
        when(htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4)).thenThrow(new ProcessingException("java.net.ConnectException: Connection refused"));
        WeasyPrintTestConversionStatusProvider weasyPrintTestConversionStatusProvider = new WeasyPrintTestConversionStatusProvider(htmlToPdfConverter);

        ConfigurationStatus status = weasyPrintTestConversionStatusProvider.getStatus(ConfigurationStatusProvider.Context.builder().build());

        assertEquals(new ConfigurationStatus("WeasyPrint Service: Test conversion", Status.ERROR, "java.net.ConnectException: Connection refused"), status);
    }
}