package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.WrapperException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class WrapperExceptionMapperTest {

    private WrapperExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WrapperExceptionMapper();
    }

    @Test
    void testToResponse_WithUnresolvableObjectException() {
        // Given
        String errorMessage = "Object with ID 'test-id' not found";
        UnresolvableObjectException cause = new UnresolvableObjectException(errorMessage);
        WrapperException wrapperException = new WrapperException(cause);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithGenericException() {
        // Given
        String errorMessage = "Generic wrapper exception occurred";
        Exception cause = new RuntimeException("Underlying cause");
        WrapperException wrapperException = new WrapperException(errorMessage, cause);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithNoCause() {
        // Given
        String errorMessage = "Wrapper exception without cause";
        WrapperException wrapperException = new WrapperException(errorMessage);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithNullMessage() {
        // Given
        WrapperException wrapperException = new WrapperException((String) null);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertNull(errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithUnresolvableObjectException_EmptyMessage() {
        // Given
        UnresolvableObjectException cause = new UnresolvableObjectException("");
        WrapperException wrapperException = new WrapperException(cause);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals("", errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithMultipleNestedExceptions() {
        // Given
        String innerMessage = "Inner exception message";
        Exception innerException = new IllegalArgumentException(innerMessage);
        String wrapperMessage = "Wrapper exception message";
        WrapperException wrapperException = new WrapperException(wrapperMessage, innerException);

        // When
        Response response = mapper.toResponse(wrapperException);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(wrapperMessage, errorEntity.getMessage());
    }

}
