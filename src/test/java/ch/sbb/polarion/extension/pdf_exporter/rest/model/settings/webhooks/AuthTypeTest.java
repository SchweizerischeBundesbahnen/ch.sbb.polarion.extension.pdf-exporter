package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.webhooks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthTypeTest {

    @Test
    void testEnumValues() {
        AuthType[] values = AuthType.values();
        assertEquals(2, values.length, "AuthType should have exactly 2 values");
        assertEquals(AuthType.BEARER_TOKEN, values[0]);
        assertEquals(AuthType.BASIC_AUTH, values[1]);
    }

    @Test
    void testAuthHeaderPrefix() {
        assertEquals("Bearer", AuthType.BEARER_TOKEN.getAuthHeaderPrefix());
        assertEquals("Basic", AuthType.BASIC_AUTH.getAuthHeaderPrefix());
    }

    @Test
    void testForNameWithExactMatch() {
        assertEquals(AuthType.BEARER_TOKEN, AuthType.forName("BEARER_TOKEN"));
        assertEquals(AuthType.BASIC_AUTH, AuthType.forName("BASIC_AUTH"));
    }

    @Test
    void testForNameWithLowercaseInput() {
        assertEquals(AuthType.BEARER_TOKEN, AuthType.forName("bearer_token"));
        assertEquals(AuthType.BASIC_AUTH, AuthType.forName("basic_auth"));
    }

    @Test
    void testForNameWithMixedCaseInput() {
        assertEquals(AuthType.BEARER_TOKEN, AuthType.forName("Bearer_Token"));
        assertEquals(AuthType.BEARER_TOKEN, AuthType.forName("bEaReR_tOkEn"));
        assertEquals(AuthType.BASIC_AUTH, AuthType.forName("Basic_Auth"));
        assertEquals(AuthType.BASIC_AUTH, AuthType.forName("bAsIc_AuTh"));
    }

    @Test
    void testForNameWithInvalidInput() {
        assertNull(AuthType.forName("INVALID"));
        assertNull(AuthType.forName("Bearer"));
        assertNull(AuthType.forName("Basic"));
        assertNull(AuthType.forName(""));
        assertNull(AuthType.forName("TOKEN"));
    }

    @Test
    void testForNameWithNull() {
        assertNull(AuthType.forName(null));
    }

    @Test
    void testEnumName() {
        assertEquals("BEARER_TOKEN", AuthType.BEARER_TOKEN.name());
        assertEquals("BASIC_AUTH", AuthType.BASIC_AUTH.name());
    }

    @Test
    void testValueOf() {
        assertEquals(AuthType.BEARER_TOKEN, AuthType.valueOf("BEARER_TOKEN"));
        assertEquals(AuthType.BASIC_AUTH, AuthType.valueOf("BASIC_AUTH"));
    }

    @Test
    void testValueOfWithInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> AuthType.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> AuthType.valueOf("bearer_token"));
    }

}
