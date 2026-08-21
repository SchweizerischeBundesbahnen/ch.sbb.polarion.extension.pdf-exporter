package ch.sbb.polarion.extension.pdf_exporter.rest;

import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.rest.controller.roles.RolesApiController;
import ch.sbb.polarion.extension.generic.rest.controller.roles.RolesInternalController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.BulkExportWidgetApiController;
import ch.sbb.polarion.extension.pdf_exporter.rest.controller.BulkExportWidgetInternalController;
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
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.UserFriendlyRuntimeExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.WrapperExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.exception.XLIFFExceptionMapper;
import ch.sbb.polarion.extension.pdf_exporter.rest.filter.ExportContextFilter;
import ch.sbb.polarion.extension.pdf_exporter.rest.filter.RolesRestrictedFilter;
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
                BulkExportWidgetApiController.class,
                BulkExportWidgetInternalController.class,
                UtilityResourcesApiController.class,
                UtilityResourcesInternalController.class,
                ConfigurationApiController.class,
                // The role endpoints are opt-in in generic: only the extensions whose settings grant
                // permissions to roles serve them. The Authorization page reads /roles to know which
                // global and project roles the current scope offers - the JSP page it replaces read
                // them server-side instead, which is why they were never registered here.
                RolesInternalController.class,
                RolesApiController.class
        );
    }

    @Override
    protected @NotNull Set<Object> getExtensionExceptionMapperSingletons() {
        return Set.of(
                new XLIFFExceptionMapper(),
                new UnresolvableObjectExceptionMapper(),
                new WrapperExceptionMapper(),
                new NoSuchElementExceptionMapper(),
                new UserFriendlyRuntimeExceptionMapper()
        );
    }

    @Override
    protected @NotNull Set<Object> getExtensionFilterSingletons() {
        return Set.of(new ExportContextFilter(), new RolesRestrictedFilter());
    }
}
