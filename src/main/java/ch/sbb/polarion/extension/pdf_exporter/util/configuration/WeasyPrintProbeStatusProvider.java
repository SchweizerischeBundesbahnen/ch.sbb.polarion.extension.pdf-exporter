package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import org.jetbrains.annotations.NotNull;

@Discoverable
public class WeasyPrintProbeStatusProvider extends ConfigurationStatusProvider {

    private static final String WEASY_PRINT_SERVICE_TEST_CONVERSION = "WeasyPrint Service: WeasyPrint probe";

    private final HtmlToPdfConverter htmlToPdfConverter;

    public WeasyPrintProbeStatusProvider() {
        this.htmlToPdfConverter = new HtmlToPdfConverter();
    }

    public WeasyPrintProbeStatusProvider(HtmlToPdfConverter htmlToPdfConverter) {
        this.htmlToPdfConverter = htmlToPdfConverter;
    }

    @Override
    public @NotNull ConfigurationStatus getStatus(@NotNull Context context) {
        try {
            htmlToPdfConverter.convert("<html><body>test html</body></html>", ConversionParams.builder().build());

            return new ConfigurationStatus(WEASY_PRINT_SERVICE_TEST_CONVERSION, Status.OK);
        } catch (Exception e) {
            return new ConfigurationStatus(WEASY_PRINT_SERVICE_TEST_CONVERSION, Status.ERROR, e.getMessage());
        }
    }

}
