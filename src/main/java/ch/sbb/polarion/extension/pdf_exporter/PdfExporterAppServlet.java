package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

/**
 * Serves the React administration app (the Vite bundle under {@code webapp/pdf-exporter-app}) and the
 * build-generated help articles next to it. The pages still on JSP keep being served by
 * {@link PdfExporterAdminUiServlet} until they are converted too.
 */
public class PdfExporterAppServlet extends GenericUiServlet {
    @Serial
    private static final long serialVersionUID = 6284519037465812093L;

    public PdfExporterAppServlet() {
        super("pdf-exporter-app");
    }
}
