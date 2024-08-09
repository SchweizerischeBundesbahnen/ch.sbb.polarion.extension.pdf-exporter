package ch.sbb.polarion.extension.pdf.exporter.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.pdf.exporter.converter.PdfConverterJobsCleaner;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.ConverterApiController;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.ConverterInternalController;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.SettingsApiController;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.SettingsInternalController;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.UtilityResourcesApiController;
import ch.sbb.polarion.extension.pdf.exporter.rest.controller.UtilityResourcesInternalController;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.NoSuchElementExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.UnresolvableObjectExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.WrapperExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.XLIFFExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.StylePackageSettings;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class PdfExporterRestApplication extends GenericRestApplication {
    private final Logger logger = Logger.getLogger(PdfExporterRestApplication.class);

    public PdfExporterRestApplication() {
        logger.debug("Creating PDF-Exporter REST Application...");

        try {
            NamedSettingsRegistry.INSTANCE.register(
                    Arrays.asList(
                            new StylePackageSettings(),
                            new HeaderFooterSettings(),
                            new CssSettings(),
                            new LocalizationSettings(),
                            new CoverPageSettings(),
                            new WebhooksSettings(),
                            new FileNameTemplateSettings()
                    )
            );
        } catch (Exception e) {
            logger.error("Error during registration of named settings", e);
        }

        try {
            PdfConverterJobsCleaner.startCleaningJob();
        } catch (Exception e) {
            logger.error("Error during starting of clearing job", e);
        }

        logger.debug("PDF-Exporter REST Application has been created");
    }

    @Override
    protected @NotNull Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new ConverterApiController(),
                new ConverterInternalController(),
                new SettingsApiController(),
                new SettingsInternalController(),
                new UtilityResourcesApiController(),
                new UtilityResourcesInternalController()
        );
    }

    @Override
    protected @NotNull Set<Object> getExtensionExceptionMapperSingletons() {
        return Set.of(
                new XLIFFExceptionMapper(),
                new UnresolvableObjectExceptionMapper(),
                new WrapperExceptionMapper(),
                new NoSuchElementExceptionMapper()
        );
    }
}