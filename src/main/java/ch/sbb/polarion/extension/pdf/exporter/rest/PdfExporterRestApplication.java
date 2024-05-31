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
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.IllegalStateExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.NoSuchElementExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.UnresolvableObjectExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.WrapperExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.rest.exception.XLIFFExceptionMapper;
import ch.sbb.polarion.extension.pdf.exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.FileNameTemplateSettings;
import ch.sbb.polarion.extension.pdf.exporter.settings.HeaderFooterSettings;
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

    // By default, separate instance of controllers will be created per request
    // To use single controller instance for all requests, the Application.getSingletons() must be overriden and return controller instances
    @Override
    @NotNull
    protected Set<Class<?>> getControllerClasses() {
        final Set<Class<?>> controllerClasses = super.getControllerClasses();
        controllerClasses.addAll(Set.of(
                ConverterApiController.class,
                ConverterInternalController.class,
                SettingsApiController.class,
                SettingsInternalController.class,
                UtilityResourcesApiController.class,
                UtilityResourcesInternalController.class
        ));
        return controllerClasses;
    }

    @Override
    @NotNull
    protected Set<Class<?>> getExceptionMappers() {
        final Set<Class<?>> exceptionMappers = super.getExceptionMappers();
        exceptionMappers.addAll(Set.of(
                XLIFFExceptionMapper.class,
                UnresolvableObjectExceptionMapper.class,
                WrapperExceptionMapper.class,
                IllegalStateExceptionMapper.class,
                NoSuchElementExceptionMapper.class
        ));
        return exceptionMappers;
    }
}