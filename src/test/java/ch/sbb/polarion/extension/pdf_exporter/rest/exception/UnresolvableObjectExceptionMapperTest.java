package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.platform.persistence.UnresolvableObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class UnresolvableObjectExceptionMapperTest {

    private UnresolvableObjectExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UnresolvableObjectExceptionMapper();
    }

    @Test
    void testToResponse_WithValidException() {
        // Given
        String errorMessage = "Object with ID 'work-item-123' cannot be resolved";
        UnresolvableObjectException exception = new UnresolvableObjectException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithNullMessage() {
        // Given
        UnresolvableObjectException exception = new UnresolvableObjectException((String) null);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertNull(errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithEmptyMessage() {
        // Given
        String errorMessage = "";
        UnresolvableObjectException exception = new UnresolvableObjectException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals("", errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithLongMessage() {
        // Given
        String errorMessage = "Object with ID 'very-long-id-that-contains-many-characters-and-detailed-information-about-the-unresolvable-object' cannot be resolved in the current context";
        UnresolvableObjectException exception = new UnresolvableObjectException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithSpecialCharactersInMessage() {
        // Given
        String errorMessage = "Object with ID 'test-äöü-€-@-#' cannot be resolved: <special&chars>";
        UnresolvableObjectException exception = new UnresolvableObjectException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    @SuppressWarnings("resource")
    void testToResponse_ResponseStatusIs404() {
        // Given
        UnresolvableObjectException exception = new UnresolvableObjectException("Test message");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(404, response.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @SuppressWarnings("resource")
    void testToResponse_ContentTypeIsJson() {
        // Given
        UnresolvableObjectException exception = new UnresolvableObjectException("Test message");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response.getMediaType());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
    }

}
