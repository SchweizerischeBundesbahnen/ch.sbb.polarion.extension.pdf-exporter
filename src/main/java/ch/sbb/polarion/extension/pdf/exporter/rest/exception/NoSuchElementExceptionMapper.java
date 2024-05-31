package ch.sbb.polarion.extension.pdf.exporter.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.NoSuchElementException;

public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {
    private final Logger logger = Logger.getLogger(NoSuchElementExceptionMapper.class);

    public Response toResponse(NoSuchElementException e) {
        logger.error("Unknown element: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
