package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Answers with the message of the failure rather than with an error id.
 * <p>
 * The default mapper of the generic extension hides the message of anything it does not know,
 * since an arbitrary exception may carry a class name or a repository path. This type says in its
 * own name that its message was written for a person to read: it names what to configure and
 * where. Hiding it behind an error id would leave the reader of the response with nothing to act
 * on, while the export dialog, which reads the state of a job, shows that same text already.
 * <p>
 * The status matches how the generic extension answers an {@code IllegalStateException}, which is
 * what these failures were before they became user friendly.
 */
@Provider
public class UserFriendlyRuntimeExceptionMapper implements ExceptionMapper<UserFriendlyRuntimeException> {
    private final Logger logger = Logger.getLogger(UserFriendlyRuntimeExceptionMapper.class);

    public Response toResponse(UserFriendlyRuntimeException e) {
        logger.error("Export cannot proceed: " + e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
