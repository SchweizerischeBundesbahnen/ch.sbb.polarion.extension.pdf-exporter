package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class NoSuchElementExceptionMapperTest {

    private NoSuchElementExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NoSuchElementExceptionMapper();
    }

    @Test
    void testToResponse_WithValidException() {
        // Given
        String errorMessage = "Element with ID 'test-123' not found";
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

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
        NoSuchElementException exception = new NoSuchElementException((String) null);

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
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

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
        String errorMessage = "No such element: The requested configuration element 'custom-report-config-v2' could not be found in the system. Please verify the element ID and try again.";
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

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
        String errorMessage = "Element '<>&\"' not found in collection";
        NoSuchElementException exception = new NoSuchElementException(errorMessage);

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
    void testToResponse_StatusCodeIs404() {
        // Given
        NoSuchElementException exception = new NoSuchElementException("Not found");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(404, response.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    @SuppressWarnings("resource")
    void testToResponse_MediaTypeIsJson() {
        // Given
        NoSuchElementException exception = new NoSuchElementException("Test message");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response.getMediaType());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
    }

}
