package ch.sbb.polarion.extension.pdf_exporter.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.pdf_exporter.PdfExporterInternalModule;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsCleaner;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.CollectionApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.CollectionInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.ConverterApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.ConverterInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.SettingsApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.SettingsInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.TestRunAttachmentsApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.TestRunAttachmentsInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.UtilityResourcesApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.UtilityResourcesInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.NoSuchElementExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.UnresolvableObjectExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.WrapperExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.XLIFFExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
        try {
            Injector injector = Guice.createInjector(new PdfExporterInternalModule());

            return Set.of(
                    injector.getInstance(ConverterApiController.class),
                    injector.getInstance(ConverterInternalController.class),
                    injector.getInstance(TestRunAttachmentsApiController.class),
                    injector.getInstance(TestRunAttachmentsInternalController.class),
                    injector.getInstance(CollectionApiController.class),
                    injector.getInstance(CollectionInternalController.class),
                    injector.getInstance(UtilityResourcesApiController.class),
                    injector.getInstance(UtilityResourcesInternalController.class),
                    injector.getInstance(SettingsApiController.class),
                    injector.getInstance(SettingsInternalController.class)
            );
        } catch (ConfigurationException e) {
            logger.error("Cannot instantiate controllers: " + e.getErrorMessages(), e);
            throw new IllegalStateException(e);
        }
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
