package ch.sbb.polarion.extension.pdf.exporter.weasyprint;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.properties.WeasyPrintConnector;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.cli.WeasyPrintExecutor;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.WeasyPrintServiceConnector;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class WeasyPrintConnectorFactory {

    public static @NotNull WeasyPrintConverter getWeasyPrintExecutor() {
        WeasyPrintConnector weasyPrintConnector = PdfExporterExtensionConfiguration.getInstance().getWeasyprintConnector();
        return getWeasyPrintExecutor(weasyPrintConnector);
    }

    public static @NotNull WeasyPrintConverter getWeasyPrintExecutor(@NotNull WeasyPrintConnector weasyPrintConnector) {
        return switch (weasyPrintConnector) {
            case SERVICE -> new WeasyPrintServiceConnector();
            case CLI -> new WeasyPrintExecutor();
        };
    }
}
