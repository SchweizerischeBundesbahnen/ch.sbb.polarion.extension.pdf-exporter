package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConverterApiControllerTest {
    @Mock
    private PdfExporterPolarionService polarionService;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @InjectMocks
    private ConverterApiController converterApiController;

    @BeforeEach
    void setup() {
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSetLogoutSkipPropertyForSingleJob() {
        converterApiController.startPdfConverterJob(ExportParams.builder().build());
        verify(requestAttributes).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        verify(polarionService).callPrivileged(any(Callable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSetLogoutSkipPropertyForMergeJob() {
        converterApiController.startMergeExportJob(List.of(ExportParams.builder().build()));
        verify(requestAttributes).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        verify(polarionService).callPrivileged(any(Callable.class));
    }

    /**
     * A start which fails gives the session back to the logout filter: no job would end it.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldReleaseSessionWhenStartFails() {
        when(polarionService.callPrivileged(any(Callable.class))).thenThrow(new IllegalStateException("start failed"));
        ExportParams exportParams = ExportParams.builder().build();

        assertThrows(IllegalStateException.class, () -> converterApiController.startPdfConverterJob(exportParams));

        verify(requestAttributes).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        verify(requestAttributes).removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST);
    }
}
