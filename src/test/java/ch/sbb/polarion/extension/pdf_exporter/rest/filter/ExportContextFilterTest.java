package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportContextFilterTest {

    private ExportContextFilter exportContextFilter;

    @Mock
    private ContainerRequestContext mockRequestContext;

    @Mock
    private ContainerResponseContext mockResponseContext;

    @Mock
    private MultivaluedMap<String, Object> mockHeaders;

    @BeforeEach
    void setUp() {
        exportContextFilter = new ExportContextFilter();
    }

    @Test
    @SneakyThrows
    void filterWhenHeaderIsPresentTest() {
        when(mockResponseContext.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.containsKey("Missing-WorkItem-Attachments-Count")).thenReturn(true);

        try (MockedStatic<ExportContext> mockedStatic = mockStatic(ExportContext.class)) {
            exportContextFilter.filter(mockRequestContext, mockResponseContext);

            verify(mockResponseContext, times(1)).getHeaders();
            verify(mockHeaders, times(1)).containsKey("Missing-WorkItem-Attachments-Count");
            mockedStatic.verify(ExportContext::clear, times(1));
        }
    }

    @Test
    @SneakyThrows
    void filterWhenHeaderIsNotPresentTest() {
        when(mockResponseContext.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.containsKey("Missing-WorkItem-Attachments-Count")).thenReturn(false);

        try (MockedStatic<ExportContext> mockedStatic = mockStatic(ExportContext.class)) {
            exportContextFilter.filter(mockRequestContext, mockResponseContext);

            verify(mockResponseContext, times(1)).getHeaders();
            verify(mockHeaders, times(1)).containsKey("Missing-WorkItem-Attachments-Count");
            mockedStatic.verifyNoInteractions();
        }
    }
}
