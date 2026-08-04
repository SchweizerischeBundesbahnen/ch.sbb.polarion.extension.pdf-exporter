package ch.sbb.polarion.extension.pdf_exporter.rest.filter;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JAX-RS resource method (or class) as restricted to the roles configured for PDF export.
 * <p>
 * The bound {@link RolesRestrictedFilter} resolves the project to authorize against from the request —
 * from a query parameter, a path parameter, or a field of the JSON request body — and rejects the request
 * with {@code 403 Forbidden} when the current user holds none of the configured roles. When no roles are
 * configured for the scope the export is unrestricted (see {@code AuthorizationSettings}).
 * <p>
 * This is intentionally self-contained so it can later be promoted to the shared {@code generic} extension.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RolesRestricted {

    /**
     * Where to read the project identifier from.
     */
    ScopeSource scopeSource() default ScopeSource.BODY;

    /**
     * Name of the query/path parameter or JSON body field holding the project id.
     */
    String scopeParam() default "projectId";

    enum ScopeSource {
        QUERY, PATH, BODY
    }
}
