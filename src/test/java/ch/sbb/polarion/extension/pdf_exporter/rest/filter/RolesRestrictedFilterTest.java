package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.platform.security.ISecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.io.ByteArrayInputStream;
import java.security.PrivilegedAction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

        @RolesRestricted(scopeSource = RolesRestricted.ScopeSource.QUERY, scopeParam = "projectId")
        public void annotatedQuery() {
        }

        @RolesRestricted(scopeSource = RolesRestricted.ScopeSource.PATH, scopeParam = "projectId")
        public void annotatedPath() {
        }

        public void notAnnotated() {
        }
    }

    @BeforeEach
    void setUp() {
        filter = new RolesRestrictedFilter(polarionService, securityService, resourceInfo);
    }

    @AfterEach
    void tearDown() {
        // Keep request-scoped state from leaking between tests (the impersonation test sets it).
        RequestContextHolder.resetRequestAttributes();
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

    @Test
    void resolvesProjectIdFromQueryParam() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(Sample.class.getMethod("annotatedQuery"));
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("projectId", "queryProject");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getQueryParameters()).thenReturn(params);
        when(polarionService.userAuthorizedForExport("queryProject")).thenReturn(true);

        filter.filter(requestContext);

        verify(polarionService).userAuthorizedForExport("queryProject");
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void resolvesProjectIdFromPathParam() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(Sample.class.getMethod("annotatedPath"));
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("projectId", "pathProject");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(params);
        when(polarionService.userAuthorizedForExport("pathProject")).thenReturn(true);

        filter.filter(requestContext);

        verify(polarionService).userAuthorizedForExport("pathProject");
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void nonJsonBodyResolvesNullProjectIdWithoutReadingStream() throws Exception {
        stubAnnotatedMethod();
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);
        when(polarionService.userAuthorizedForExport(null)).thenReturn(true);

        filter.filter(requestContext);

        verify(polarionService).userAuthorizedForExport(null);
        verify(requestContext, never()).setEntityStream(any());
    }

    @Test
    void runsCheckAsRequestSubjectOnApiPath() throws Exception {
        stubAnnotatedMethod();
        stubJsonBody("{\"projectId\":\"apiProject\"}");

        Subject subject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(AuthenticationFilter.USER_SUBJECT)).thenReturn(subject);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(securityService.doAsUser(eq(subject), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(1)).run());
        when(polarionService.userAuthorizedForExport("apiProject")).thenReturn(true);

        filter.filter(requestContext);

        verify(securityService).doAsUser(eq(subject), any(PrivilegedAction.class));
        verify(polarionService).userAuthorizedForExport("apiProject");
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
