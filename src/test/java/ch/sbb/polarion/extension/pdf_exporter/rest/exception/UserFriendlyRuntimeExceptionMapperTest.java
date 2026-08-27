package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UserFriendlyRuntimeExceptionMapperTest {

    @Test
    void answersWithTheMessageRatherThanAnErrorId() {
        // the default mapper of the generic extension hides the message of an unknown failure behind an
        // error id, which would leave a caller of the REST API with nothing to act on
        String message = "The Polarion secret 'no.such.secret' is empty or does not exist";

        Response response = new UserFriendlyRuntimeExceptionMapper().toResponse(new UserFriendlyRuntimeException(message));

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(message, assertInstanceOf(ErrorEntity.class, response.getEntity()).getMessage());
    }
}
