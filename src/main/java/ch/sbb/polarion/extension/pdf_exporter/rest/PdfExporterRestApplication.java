package ch.sbb.polarion.extension.pdf_exporter.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.CollectionApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.CollectionInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.ConfigurationApiController;
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
import ch.sbb.polarion.extension.pdf_exporter.rest.filter.ExportContextFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PdfExporterRestApplication extends GenericRestApplication {

    @Override
    protected @NotNull Set<Class<?>> getExtensionControllerClasses() {
        return Set.of(
                ConverterApiController.class,
                ConverterInternalController.class,
                SettingsApiController.class,
                SettingsInternalController.class,
                TestRunAttachmentsApiController.class,
                TestRunAttachmentsInternalController.class,
                CollectionApiController.class,
                CollectionInternalController.class,
                UtilityResourcesApiController.class,
                UtilityResourcesInternalController.class,
                ConfigurationApiController.class
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

    @Override
    protected @NotNull Set<Object> getExtensionFilterSingletons() {
        return Set.of(new ExportContextFilter());
    }
}
