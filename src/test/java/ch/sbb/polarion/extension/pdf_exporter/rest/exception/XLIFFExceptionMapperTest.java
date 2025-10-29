package ch.sbb.polarion.extension.pdf_exporter.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import net.sf.okapi.lib.xliff2.XLIFFException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class XLIFFExceptionMapperTest {

    private XLIFFExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new XLIFFExceptionMapper();
    }

    static Stream<Arguments> provideErrorMessages() {
        return Stream.of(
                Arguments.of("Valid exception with simple message", "XLIFF format error: Invalid structure"),
                Arguments.of("Long error message", "XLIFF format error: The document structure is invalid. Expected element 'xliff' but found 'invalid'. Please ensure the XLIFF document follows the XLIFF 2.0 specification"),
                Arguments.of("Special characters in message", "XLIFF error: Invalid char '<>&\"' at line 42")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideErrorMessages")
    void testToResponse_WithVariousMessages(String testName, String errorMessage) {
        // Given
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
