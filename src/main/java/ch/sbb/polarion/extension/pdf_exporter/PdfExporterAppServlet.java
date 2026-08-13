package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

/**
 * Serves the React administration app (the Vite bundle under {@code webapp/pdf-exporter-app}), the
 * build-generated help articles next to it, and the administration menu icons that
 * {@code hivemodule.xml} and the report widgets point at. Every administration page of the extension is
 * served from here.
 */
public class PdfExporterAppServlet extends GenericUiServlet {
    @Serial
    private static final long serialVersionUID = 6284519037465812093L;

    public PdfExporterAppServlet() {
        super("pdf-exporter-app");
    }
}
