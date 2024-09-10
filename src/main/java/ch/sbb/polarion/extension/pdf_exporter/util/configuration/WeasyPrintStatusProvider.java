package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import ch.sbb.polarion.extension.pdf_exporter.converter.HtmlToPdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Discoverable
public class WeasyPrintStatusProvider extends ConfigurationStatusProvider {

    public static final List<String> WEASY_PRINT = List.of("WeasyPrint Python", "WeasyPrint", "WeasyPrint Service");

    private static @NotNull ConfigurationStatus createWeasyPrintStatus(@NotNull String name, @Nullable String description) {
        if (description == null) {
            return new ConfigurationStatus(name, Status.WARNING, "Unknown");
        } else {
            return new ConfigurationStatus(name, Status.OK, description);
        }
    }

    @Override
    public @NotNull List<ConfigurationStatus> getStatuses(@NotNull Context context) {
        try {
            HtmlToPdfConverter htmlToPdfConverter = new HtmlToPdfConverter();
            WeasyPrintServiceConnector weasyPrintServiceConnector = new WeasyPrintServiceConnector();

            WeasyPrintInfo weasyPrintInfo = weasyPrintServiceConnector.getWeasyPrintInfo();
            htmlToPdfConverter.convert("<html><body>test html</body></html>", Orientation.PORTRAIT, PaperSize.A4);

            return List.of(
                    createWeasyPrintStatus(WEASY_PRINT.get(0), weasyPrintInfo.getPython()),
                    createWeasyPrintStatus(WEASY_PRINT.get(1), weasyPrintInfo.getWeasyprint()),
                    createWeasyPrintStatus(WEASY_PRINT.get(2), weasyPrintInfo.getWeasyprintService())
            );
        } catch (Exception e) {
            return List.of(new ConfigurationStatus(WEASY_PRINT.get(1), Status.ERROR, e.getMessage()));
        }
    }
}
