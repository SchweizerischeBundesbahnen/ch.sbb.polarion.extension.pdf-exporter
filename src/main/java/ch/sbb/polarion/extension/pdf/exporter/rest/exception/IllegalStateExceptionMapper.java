package ch.sbb.polarion.extension.pdf.exporter.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {
    private final Logger logger = Logger.getLogger(IllegalStateExceptionMapper.class);

    public Response toResponse(IllegalStateException e) {
        logger.error("Illegal state error: " + e.getMessage(), e);
        return Response.status(Response.Status.CONFLICT.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
