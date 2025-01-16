package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
class ExportContextFilterTest {

    private ExportContextFilter exportContextFilter;
    private ContainerRequestContext mockRequestContext;
    private ContainerResponseContext mockResponseContext;

    @BeforeEach
    void setUp() {
        exportContextFilter = new ExportContextFilter();
        mockRequestContext = mock(ContainerRequestContext.class);
        mockResponseContext = mock(ContainerResponseContext.class);
    }

    @Test
    @SneakyThrows
    void filterClearsExportContextTest() {
        ExportContext.addWorkItemIDsWithMissingAttachment("Workitem1");
        ExportContext.addWorkItemIDsWithMissingAttachment("Workitem2");

        exportContextFilter.filter(mockRequestContext, mockResponseContext);

        assertTrue(ExportContext.getWorkItemIDsWithMissingAttachment().isEmpty());
    }
}
