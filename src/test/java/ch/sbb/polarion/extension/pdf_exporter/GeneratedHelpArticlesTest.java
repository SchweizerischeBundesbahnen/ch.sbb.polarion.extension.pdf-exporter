package ch.sbb.polarion.extension.pdf_exporter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins where the build-generated help articles land.
 * <p>
 * The three of them are written by markdown2html into the directory
 * {@code markdown2html-maven-plugin.extensionContextAdminHtml} points at - a property whose name
 * predates the React apps: as soon as {@code ui/} exists, the generic parent's {@code vite-ui} profile
 * redefines it to the <em>app</em> webapp. Two things then depend on that, silently:
 * <ul>
 *   <li>generic's {@code /readme}, {@code /user-guide} and {@code /disclaimer} endpoints read their
 *       article from the classpath, searching {@code -app} before {@code -admin};</li>
 *   <li>each answers with an empty body when its article is absent, which the pages render as their
 *       "not generated" message.</li>
 * </ul>
 * A build that stops generating the articles is therefore silent - every page keeps loading and just
 * shows nothing. This test is what turns that into a red build instead. It is deterministic: since markdown2html 1.7.x the
 * markdown is rendered locally, with no GitHub API call and no token.
 */
class GeneratedHelpArticlesTest {

    private void assertArticleGenerated(String fileName) {
        String resource = "/webapp/pdf-exporter-app/html/" + fileName;
        assertNotNull(getClass().getResource(resource),
                resource + " is missing: markdown2html no longer writes the generated articles into the app webapp, "
                        + "so the About, User Guide and Usage Disclaimer pages render their 'not generated' message");
    }

    @Test
    void aboutArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("about.html");
    }

    @Test
    void userGuideArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("user-guide.html");
    }

    @Test
    void disclaimerArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("disclaimer.html");
    }

}
