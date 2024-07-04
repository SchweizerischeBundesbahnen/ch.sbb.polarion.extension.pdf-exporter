package ch.sbb.polarion.extension.pdf.exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.WrapperException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class WrapperExceptionMapper implements ExceptionMapper<WrapperException> {
    private final Logger logger = Logger.getLogger(WrapperExceptionMapper.class);

    public Response toResponse(WrapperException e) {
        logger.error("Polarion wrapper exception: " + e.getMessage(), e);
        if (e.getCause() instanceof UnresolvableObjectException) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                    .entity(new ErrorEntity(e.getCause().getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .entity(new ErrorEntity(e.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
