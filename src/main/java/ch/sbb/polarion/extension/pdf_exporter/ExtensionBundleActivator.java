package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.GenericBundleActivator;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageVisibilitySettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.core.util.logging.Logger;
import org.osgi.framework.BundleContext;

import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("unused")
public class ExtensionBundleActivator extends GenericBundleActivator {
    private final Logger logger = Logger.getLogger(ExtensionBundleActivator.class);

    @Override
    protected void onStart(BundleContext context) {
        initializeFeatures();
    }

    @Override
    protected Map<String, IFormExtension> getExtensions() {
        return Map.of("pdf-exporter", new PdfExporterFormExtension());
    }

    @Override
    public void stop(BundleContext context) {
        PdfConverterJobsService.shutdown();
        super.stop(context);
    }

    private void initializeFeatures() {
        try {
            NamedSettingsRegistry.INSTANCE.register(
                    Arrays.asList(
                            new StylePackageSettings(),
                            new HeaderFooterSettings(),
                            new CssSettings(),
                            new LocalizationSettings(),
                            new CoverPageSettings(),
                            new WebhooksSettings(),
                            new FileNameTemplateSettings(),
                            new AuthorizationSettings(),
                            new StylePackageVisibilitySettings()
                    )
            );
        } catch (Exception e) {
            logger.error("Error during registration of named settings", e);
        }

        try {
            PdfConverterJobsService.startCleaner();
        } catch (Exception e) {
            logger.error("Error during starting of clearing job", e);
        }
    }
}
