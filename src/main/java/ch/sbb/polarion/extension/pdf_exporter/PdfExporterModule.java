package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericModule;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;

public class PdfExporterModule extends GenericModule {
    public static final String PDF_EXPORTER_FORM_EXTENSION_ID = "pdf-exporter";

    @Override
    protected FormExtensionContribution getFormExtensionContribution() {
        return new FormExtensionContribution(PdfExporterFormExtension.class, PDF_EXPORTER_FORM_EXTENSION_ID);
    }
}
