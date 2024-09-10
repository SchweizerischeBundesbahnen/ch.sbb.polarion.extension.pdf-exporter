package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

public class PdfExporterUiServlet extends GenericUiServlet {
    @Serial
    private static final long serialVersionUID = 8956634237920845540L;

    public PdfExporterUiServlet() {
        super("pdf-exporter");
    }
}
