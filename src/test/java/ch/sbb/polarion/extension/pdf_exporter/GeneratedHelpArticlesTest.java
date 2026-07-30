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
 *   <li>generic's {@code /readme} and {@code /user-guide} endpoints read their article from the
 *       classpath, searching {@code -app} before {@code -admin};</li>
 *   <li>the Usage Disclaimer page has no endpoint to read from and fetches
 *       {@code /polarion/pdf-exporter-app/ui/html/disclaimer.html} over HTTP, which only
 *       {@code PdfExporterAppServlet} can serve - from the app webapp and nowhere else.</li>
 * </ul>
 * Should the destination ever move back under {@code -admin}, the REST endpoints keep working through
 * their fallback and only the Disclaimer page breaks, showing its "not generated" message. This test
 * is what turns that into a red build instead. It is deterministic: since markdown2html 1.7.x the
 * markdown is rendered locally, with no GitHub API call and no token.
 */
class GeneratedHelpArticlesTest {

    private void assertArticleGenerated(String fileName) {
        String resource = "/webapp/pdf-exporter-app/html/" + fileName;
        assertNotNull(getClass().getResource(resource),
                resource + " is missing: markdown2html no longer writes the generated articles into the app webapp, "
                        + "which breaks the Usage Disclaimer page (it fetches disclaimer.html from there over HTTP)");
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
