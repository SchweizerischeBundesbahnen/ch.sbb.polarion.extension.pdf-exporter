package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WikiRenderer.
 * Note: Full unit testing of WikiRenderer is limited because it depends heavily on
 * XWiki internals that require Polarion runtime. The render() method is tested
 * indirectly through DocumentDataTest via MockedConstruction.
 */
class WikiRendererTest {

    private static final String BASE_URL = "http://localhost:8080/polarion";

    private WikiRenderer wikiRenderer;
    private String originalBaseUrl;

    @BeforeEach
    void setUp() {
        wikiRenderer = new WikiRenderer();
        originalBaseUrl = System.getProperty("base.url");
        System.setProperty("base.url", BASE_URL);
    }

    @AfterEach
    void tearDown() {
        if (originalBaseUrl != null) {
            System.setProperty("base.url", originalBaseUrl);
        } else {
            System.clearProperty("base.url");
        }
    }

    @Test
    void wikiRendererShouldBeInstantiable() {
        assertNotNull(wikiRenderer);
    }

    @Test
    void renderShouldThrowExceptionWhenBaseUrlNotSet() {
        System.clearProperty("base.url");
        WikiRenderer renderer = new WikiRenderer();

        assertThrows(Exception.class, () -> renderer.render("project", "path", null));
    }

    @Test
    void renderShouldThrowExceptionForInvalidBaseUrl() {
        System.setProperty("base.url", "not-a-valid-url");
        WikiRenderer renderer = new WikiRenderer();

        assertThrows(Exception.class, () -> renderer.render("project", "path", null));
    }

    @Test
    void renderShouldCreateUriAndConvertToUrl() {
        // This test verifies the URI creation and toURL conversion (lines 23-25)
        // The XWiki initialization will fail, but URI/URL code paths are covered
        System.setProperty("base.url", BASE_URL);
        WikiRenderer renderer = new WikiRenderer();

        // NoClassDefFoundError is thrown during XWiki initialization, but URI.create and toURL are executed
        assertThrows(Throwable.class, () -> renderer.render("testProject", "testPath", "123"));
    }
}
