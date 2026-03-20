package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import com.helger.css.decl.CSSDeclarationList;
import com.polarion.core.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CssUtilsTest {

    private Logger originalLogger;
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        originalLogger = Logger.getLogger(CssUtils.class);
        mockLogger = mock(Logger.class);
        CssUtils.setLogger(mockLogger);
    }

    @AfterEach
    void tearDown() {
        CssUtils.setLogger(originalLogger);
    }

    @Test
    void doesntFailOnGettingNotExistingValueTest() {
        CSSDeclarationList cssStyles = new CSSDeclarationList();
        assertDoesNotThrow(() -> {
            String width = CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH);
            assertEquals("", width);
        });
    }

    @Test
    void getPropertyValueTest() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("width: 100px");
        String width = CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH);
        assertEquals("100px", width);
    }

    @Test
    void setPropertyValueTest() {
        CSSDeclarationList cssStyles = new CSSDeclarationList();
        CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, "auto");
        assertEquals("auto", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }

    @Test
    void overwritePropertyValueTest() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("width: auto");
        CssUtils.setPropertyValue(cssStyles, CssProp.WIDTH, "100px");
        assertEquals("100px", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }

    @Test
    void parseDeclarationsReturnsEmptyListForEmptyString() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("");
        assertNotNull(cssStyles);
        assertTrue(cssStyles.getAllDeclarations().isEmpty());
    }

    @Test
    void parseDeclarationsHandlesPercentValues() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("width: 50%");
        assertEquals("50%", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }

    @Test
    void parseDeclarationsHandlesMultiplePropertiesWithPercent() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("width: 50%; height: 100%");
        assertEquals("50%", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
        assertEquals("100%", CssUtils.getPropertyValue(cssStyles, CssProp.HEIGHT));
    }

    @Test
    void parseDeclarationsHandlesPageBreakInsideWithOtherProperties() {
        CSSDeclarationList cssStyles = CssUtils.parseDeclarations("page-break-inside:avoid; width:50%");
        assertEquals("avoid", CssUtils.getPropertyValue(cssStyles, CssProp.PAGE_BREAK_INSIDE));
        assertEquals("50%", CssUtils.getPropertyValue(cssStyles, CssProp.WIDTH));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "width: 50%broken",
            "width: 50%%",
            "%%%",
            "background: url('data:image/svg+xml,%3Csvg%3E')",
            ": invalid",
            "not-css-at-all"
    })
    void parseDeclarationsDoesNotThrowOnInvalidCss(String invalidCss) {
        assertDoesNotThrow(() -> {
            CSSDeclarationList result = CssUtils.parseDeclarations(invalidCss);
            assertNotNull(result);
        });
    }

    @Test
    void parseDeclarationsLogsWarningOnInvalidCss() {
        CssUtils.parseDeclarations("%%%");

        verify(mockLogger, atLeastOnce()).warn(argThat((String msg) ->
                msg.contains("Failed to parse CSS") && msg.contains("%%%")));
    }

    @Test
    void parseDeclarationsDoesNotLogWarningForValidCss() {
        CssUtils.parseDeclarations("width: 50%; page-break-inside: avoid");

        verify(mockLogger, never()).warn(Mockito.any(Object.class));
    }

    @Test
    void parseDeclarationsTruncatesLongStyleInLog() {
        String longStyle = "x".repeat(300) + "%%%";

        CssUtils.parseDeclarations(longStyle);

        verify(mockLogger, atLeastOnce()).warn(argThat((String msg) ->
                msg.contains("truncated") && msg.contains("total length=" + longStyle.length())));
    }
}
