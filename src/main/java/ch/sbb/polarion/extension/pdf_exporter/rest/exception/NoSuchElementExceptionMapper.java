package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.NoSuchElementException;

public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {
    private final Logger logger = Logger.getLogger(NoSuchElementExceptionMapper.class);

    public Response toResponse(NoSuchElementException e) {
        logger.error("Unknown element: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
