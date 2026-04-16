package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;

import static org.junit.jupiter.api.Assertions.*;

class LinkRoleDirectionTest {
    @Test
    void testFromStringValidValues() {
        for (LinkRoleDirection direction : LinkRoleDirection.values()) {
            String name = direction.name().toLowerCase();
            LinkRoleDirection result = LinkRoleDirection.fromString(name);
            assertEquals(direction, result, "Should parse " + name + " correctly");
        }
    }

    @Test
    void testFromStringNull() {
        assertNull(LinkRoleDirection.fromString(null), "Should return null for null input");
    }

    @Test
    void testFromStringInvalidValue() {
        String invalid = "invalid_direction";
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> LinkRoleDirection.fromString(invalid));
        assertEquals(400, ex.getResponse().getStatus());
        assertTrue(ex.getResponse().getEntity().toString().contains(invalid));
    }
}
