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

import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
    void shouldSetLogoutSkipProperty() {
        converterApiController.startPdfConverterJob(ExportParams.builder().build());
        verify(requestAttributes).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        verify(polarionService).callPrivileged(any(Callable.class));
    }
}
