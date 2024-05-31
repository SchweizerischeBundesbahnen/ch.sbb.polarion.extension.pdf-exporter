package ch.sbb.polarion.extension.pdf.exporter;

import ch.sbb.polarion.extension.generic.GenericUiServlet;
import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;

import java.io.Serial;

public class PdfExporterAdminUiServlet extends GenericUiServlet {

    @Serial
    private static final long serialVersionUID = -6337912330074718317L;

    public PdfExporterAdminUiServlet() {
        super("pdf-exporter-admin");
        CurrentExtensionConfiguration.getInstance().setExtensionConfiguration(PdfExporterExtensionConfiguration.getInstance());
    }
}
