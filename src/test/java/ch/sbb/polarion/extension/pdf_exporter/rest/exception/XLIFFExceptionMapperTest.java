package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import net.sf.okapi.lib.xliff2.XLIFFException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class XLIFFExceptionMapperTest {

    private XLIFFExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new XLIFFExceptionMapper();
    }

    @Test
    void testToResponse_WithValidException() {
        // Given
        String errorMessage = "XLIFF format error: Invalid structure";
        XLIFFException exception = new XLIFFException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithNullMessage() {
        // Given
        XLIFFException exception = new XLIFFException((String) null);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertNull(errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithEmptyMessage() {
        // Given
        String errorMessage = "";
        XLIFFException exception = new XLIFFException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals("", errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithLongMessage() {
        // Given
        String errorMessage = "XLIFF format error: The document structure is invalid. Expected element 'xliff' but found 'invalid'. Please ensure the XLIFF document follows the XLIFF 2.0 specification";
        XLIFFException exception = new XLIFFException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void testToResponse_WithSpecialCharactersInMessage() {
        // Given
        String errorMessage = "XLIFF error: Invalid char '<>&\"' at line 42";
        XLIFFException exception = new XLIFFException(errorMessage);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertNotNull(response);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    @SuppressWarnings("resource")
    void testToResponse_ReturnsCorrectStatusCode() {
        // Given
        XLIFFException exception = new XLIFFException("Test error");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(400, response.getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

}
