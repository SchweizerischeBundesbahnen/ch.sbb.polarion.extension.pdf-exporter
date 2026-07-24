package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.platform.security.ISecurityService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolesRestrictedFilterTest {

    @Mock
    private PdfExporterPolarionService polarionService;
    @Mock
    private ISecurityService securityService;
    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private ContainerRequestContext requestContext;

    private RolesRestrictedFilter filter;

    @SuppressWarnings({"unused", "java:S1186"})
    static class Sample {
        @RolesRestricted
        public void annotated() {
        }

        public void notAnnotated() {
        }
    }

    @BeforeEach
    void setUp() {
        filter = new RolesRestrictedFilter(polarionService, securityService, resourceInfo);
    }

    @Test
    void skipsWhenMethodNotAnnotated() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(Sample.class.getMethod("notAnnotated"));
        when(resourceInfo.getResourceClass()).thenReturn((Class) Sample.class);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
        verify(polarionService, never()).userAuthorizedForExport(anyString());
    }

    @Test
    void allowsAuthorizedUserAndExtractsProjectIdFromBody() throws Exception {
        stubAnnotatedMethod();
        stubJsonBody("{\"projectId\":\"myProject\"}");
        when(polarionService.userAuthorizedForExport("myProject")).thenReturn(true);

        filter.filter(requestContext);

        verify(polarionService).userAuthorizedForExport("myProject");
        verify(requestContext, never()).abortWith(any());
        // body must be restored for the resource method
        verify(requestContext).setEntityStream(any());
    }

    @Test
    void rejectsUnauthorizedUserWithForbidden() throws Exception {
        stubAnnotatedMethod();
        stubJsonBody("{\"projectId\":\"myProject\"}");
        when(polarionService.userAuthorizedForExport("myProject")).thenReturn(false);

        // Thrown (not abortWith) so the generic ForbiddenExceptionMapper renders a JSON error body.
        assertThatThrownBy(() -> filter.filter(requestContext)).isInstanceOf(ForbiddenException.class);
        verify(requestContext, never()).abortWith(any());
    }

    private void stubAnnotatedMethod() throws NoSuchMethodException {
        when(resourceInfo.getResourceMethod()).thenReturn(Sample.class.getMethod("annotated"));
        lenient().when(resourceInfo.getResourceClass()).thenReturn((Class) Sample.class);
    }

    private void stubJsonBody(String json) {
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.getEntityStream()).thenReturn(new ByteArrayInputStream(json.getBytes()));
    }
}
