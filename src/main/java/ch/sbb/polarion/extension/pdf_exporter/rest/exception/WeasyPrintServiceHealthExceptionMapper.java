package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import ch.sbb.polarion.extension.pdf_exporter.exception.WeasyPrintServiceHealthException;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WeasyPrintServiceHealthExceptionMapper implements ExceptionMapper<WeasyPrintServiceHealthException> {
    private final Logger logger = Logger.getLogger(WeasyPrintServiceHealthExceptionMapper.class);

    @Override
    public Response toResponse(WeasyPrintServiceHealthException e) {
        logger.error("WeasyPrint service error: " + e.getMessage(), e);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
