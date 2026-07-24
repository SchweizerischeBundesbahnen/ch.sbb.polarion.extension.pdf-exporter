package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import ch.sbb.polarion.extension.generic.util.RequestContextUtil;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/**
 * Enforces {@link RolesRestricted}: resolves the project id from the request (query/path/body), then rejects
 * the request with {@code 403 Forbidden} when the current user is not authorized to export PDF for that project.
 * Runs at {@link Priorities#AUTHORIZATION} so it executes after authentication.
 */
@RolesRestricted
@Provider
@Priority(Priorities.AUTHORIZATION)
public class RolesRestrictedFilter implements ContainerRequestFilter {
    private static final Logger logger = Logger.getLogger(RolesRestrictedFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PdfExporterPolarionService polarionService;
    private final ISecurityService securityService;

    @Context
    private ResourceInfo resourceInfo;

    public RolesRestrictedFilter() {
        this.polarionService = new PdfExporterPolarionService();
        this.securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
    }

    RolesRestrictedFilter(@NotNull PdfExporterPolarionService polarionService, @NotNull ISecurityService securityService, @NotNull ResourceInfo resourceInfo) {
        this.polarionService = polarionService;
        this.securityService = securityService;
        this.resourceInfo = resourceInfo;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RolesRestricted annotation = resolveAnnotation();
        if (annotation == null) {
            return;
        }

        String projectId = resolveProjectId(requestContext, annotation);
        if (!isAuthorized(projectId)) {
            // Throw (rather than abortWith) so the generic ForbiddenExceptionMapper renders a JSON
            // ErrorEntity {"message": ...}, consistent with every other error response in the app.
            throw new ForbiddenException("Current user is not allowed to export PDF for this project");
        }
    }

    /**
     * Runs the role check as the effective request user. On the token-authenticated API path the user is
     * only impersonated later (inside {@code callPrivileged}), so at filter time we must evaluate the check
     * within {@code doAsUser(<request subject>)}; on the browser/session path no subject is stored and the
     * ambient current user already applies.
     */
    private boolean isAuthorized(@Nullable String projectId) {
        Subject subject = requestSubject();
        if (subject != null) {
            return Boolean.TRUE.equals(securityService.doAsUser(subject,
                    (PrivilegedAction<Boolean>) () -> polarionService.userAuthorizedForExport(projectId)));
        }
        return polarionService.userAuthorizedForExport(projectId);
    }

    @SuppressWarnings("java:S1166") // absence of a request subject is the normal browser-session case
    private @Nullable Subject requestSubject() {
        try {
            return RequestContextUtil.getUserSubject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private @Nullable RolesRestricted resolveAnnotation() {
        Method method = resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(RolesRestricted.class)) {
            return method.getAnnotation(RolesRestricted.class);
        }
        Class<?> resourceClass = resourceInfo.getResourceClass();
        return resourceClass != null ? resourceClass.getAnnotation(RolesRestricted.class) : null;
    }

    private @Nullable String resolveProjectId(@NotNull ContainerRequestContext requestContext, @NotNull RolesRestricted annotation) throws IOException {
        return switch (annotation.scopeSource()) {
            case QUERY -> requestContext.getUriInfo().getQueryParameters().getFirst(annotation.scopeParam());
            case PATH -> requestContext.getUriInfo().getPathParameters().getFirst(annotation.scopeParam());
            case BODY -> resolveFromBody(requestContext, annotation.scopeParam());
        };
    }

    private @Nullable String resolveFromBody(@NotNull ContainerRequestContext requestContext, @NotNull String field) throws IOException {
        if (!requestContext.hasEntity() || !isJson(requestContext.getMediaType())) {
            return null;
        }
        // Buffer the body so the resource method can still read it after we peek at it.
        byte[] body = requestContext.getEntityStream().readAllBytes();
        requestContext.setEntityStream(new ByteArrayInputStream(body));
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(body);
            JsonNode value = node.get(field);
            return value != null && !value.isNull() ? value.asText() : null;
        } catch (IOException e) {
            // Malformed body — let the resource method's own deserialization produce the proper error.
            logger.debug("Could not parse request body to resolve '" + field + "' for authorization: " + e.getMessage());
            return null;
        }
    }

    private boolean isJson(@Nullable MediaType mediaType) {
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }
}
