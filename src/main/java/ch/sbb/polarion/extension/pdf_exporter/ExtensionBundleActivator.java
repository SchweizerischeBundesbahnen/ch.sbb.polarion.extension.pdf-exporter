package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericBundleActivator;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsCleaner;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import org.osgi.framework.BundleContext;

import java.util.Map;

@SuppressWarnings("unused")
public class ExtensionBundleActivator extends GenericBundleActivator {

    @Override
    protected Map<String, IFormExtension> getExtensions() {
        return Map.of("pdf-exporter", new PdfExporterFormExtension());
    }

    @Override
    public void stop(BundleContext context) {
        PdfConverterJobsCleaner.stopCleaningJob();
        super.stop(context);
    }
}
