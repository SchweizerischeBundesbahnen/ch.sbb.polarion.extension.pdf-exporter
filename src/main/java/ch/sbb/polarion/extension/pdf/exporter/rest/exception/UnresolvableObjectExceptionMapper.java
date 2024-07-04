package ch.sbb.polarion.extension.pdf.exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.UnresolvableObjectException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class UnresolvableObjectExceptionMapper implements ExceptionMapper<UnresolvableObjectException> {
    private final Logger logger = Logger.getLogger(UnresolvableObjectExceptionMapper.class);

    public Response toResponse(UnresolvableObjectException e) {
        logger.error("Polarion object cannot be resolved: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
