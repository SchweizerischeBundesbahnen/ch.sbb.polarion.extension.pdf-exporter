package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import com.helger.css.decl.CSSDeclarationList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssUtilsTest {

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
        Logger cssUtilsLogger = Logger.getLogger(CssUtils.class.getName());
        List<LogRecord> logRecords = new ArrayList<>();
        Handler testHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        cssUtilsLogger.addHandler(testHandler);
        try {
            CssUtils.parseDeclarations("%%%");

            assertFalse(logRecords.isEmpty(), "Expected warning log records for invalid CSS");
            assertTrue(logRecords.stream().allMatch(r -> r.getLevel() == Level.WARNING));
            assertTrue(logRecords.stream().anyMatch(r -> r.getMessage().contains("%%%")));
        } finally {
            cssUtilsLogger.removeHandler(testHandler);
        }
    }

    @Test
    void parseDeclarationsDoesNotLogWarningForValidCss() {
        Logger cssUtilsLogger = Logger.getLogger(CssUtils.class.getName());
        List<LogRecord> logRecords = new ArrayList<>();
        Handler testHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        cssUtilsLogger.addHandler(testHandler);
        try {
            CssUtils.parseDeclarations("width: 50%; page-break-inside: avoid");

            assertTrue(logRecords.isEmpty(), "No warnings expected for valid CSS");
        } finally {
            cssUtilsLogger.removeHandler(testHandler);
        }
    }
}
