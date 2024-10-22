package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class WeasyPrintTestConversionStatusProvider extends ConfigurationStatusProvider {

    private static final String WEASY_PRINT_SERVICE_TEST_CONVERSION = "WeasyPrint Service: Test conversion";

    private final HtmlToPdfConverter htmlToPdfConverter;

    public WeasyPrintTestConversionStatusProvider() {
        this.htmlToPdfConverter = new HtmlToPdfConverter();
    }

    public WeasyPrintTestConversionStatusProvider(HtmlToPdfConverter htmlToPdfConverter) {
        this.htmlToPdfConverter = htmlToPdfConverter;
    }

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        try {
            htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4);

            return new ConfigurationStatus(WEASY_PRINT_SERVICE_TEST_CONVERSION, Status.OK);
        } catch (Exception e) {
            return new ConfigurationStatus(WEASY_PRINT_SERVICE_TEST_CONVERSION, Status.ERROR, e.getMessage());
        }
    }

}
