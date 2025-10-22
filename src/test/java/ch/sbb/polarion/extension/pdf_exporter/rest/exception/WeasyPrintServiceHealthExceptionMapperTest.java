package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import ch.sbb.polarion.extension.pdf_exporter.exception.WeasyPrintServiceHealthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WeasyPrintServiceHealthExceptionMapperTest {

    private WeasyPrintServiceHealthExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeasyPrintServiceHealthExceptionMapper();
    }

    @Test
    void shouldMapExceptionToServiceUnavailableResponse() {
        // Given: WeasyPrint service exception
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException("Service connection failed", new RuntimeException("Connection timeout"));

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: Response has SERVICE_UNAVAILABLE status
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
    }

    @Test
    void shouldIncludeErrorMessageInResponse() {
        // Given: WeasyPrint service exception with message
        String errorMessage = "Failed to retrieve WeasyPrint service health";
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException(errorMessage, new RuntimeException("Connection timeout"));

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: Response entity contains error message
        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(errorMessage, errorEntity.getMessage());
    }

    @Test
    void shouldHandleExceptionWithCause() {
        // Given: WeasyPrint service exception with cause
        IllegalStateException cause = new IllegalStateException("Connection timeout");
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException(
                "Failed to retrieve WeasyPrint service health",
                cause
        );

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: Response contains main exception message (not cause)
        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals("Failed to retrieve WeasyPrint service health", errorEntity.getMessage());
    }

    @Test
    void shouldHandleSpecialCharactersInErrorMessage() {
        // Given: Exception message with special characters
        String complexMessage = "Service error: \"quoted\" text with\nnewlines and\ttabs";
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException(complexMessage, new RuntimeException("Connection timeout"));

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: ErrorEntity properly handles special characters (Jackson will escape them)
        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(complexMessage, errorEntity.getMessage());
    }

    @Test
    void shouldHandleWindowsPathInErrorMessage() {
        // Given: Exception message with Windows path
        String pathMessage = "Path \"C:\\Program Files\\WeasyPrint\" not found";
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException(pathMessage, new RuntimeException("Connection timeout"));

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: ErrorEntity contains the path message
        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
        assertEquals(pathMessage, errorEntity.getMessage());
    }

    @Test
    void shouldHandleNullMessage() {
        // Given: Exception with null message
        WeasyPrintServiceHealthException exception = new WeasyPrintServiceHealthException(null, new RuntimeException("Connection timeout"));

        // When: Exception is mapped to response
        Response response = mapper.toResponse(exception);

        // Then: Response is created (ErrorEntity handles null)
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        ErrorEntity errorEntity = (ErrorEntity) response.getEntity();
        assertNotNull(errorEntity);
    }
}
