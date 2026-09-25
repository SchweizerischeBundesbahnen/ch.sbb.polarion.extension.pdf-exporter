package ch.sbb.polarion.extension.pdf_exporter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins where the build-generated help articles land.
 * <p>
 * markdown2html writes all seven of them into the app webapp's {@code html/} directory, the one
 * {@code markdown2html-maven-plugin.extensionContextAdminHtml} points at - a property whose name predates the
 * React apps and which the generic parent points at the <em>app</em> webapp. The pages read them in two ways, and
 * both depend on that directory silently:
 * <ul>
 *   <li>{@code about.html} and {@code disclaimer.html} are read from the classpath by generic's {@code /readme}
 *       and {@code /disclaimer} endpoints, which answer with an empty body when the article is absent;</li>
 *   <li>the five documentation-site articles are no REST resource at all: the app fetches each as the static file
 *       {@code html/<id>.html} next to its bundle, and the build indexes the same files for the documentation
 *       search.</li>
 * </ul>
 * Either way a missing article is not an error at runtime - the page just renders its "not generated" message.
 * This test is what turns a build that stops generating one into a red build instead. It is deterministic: since
 * markdown2html 1.7.x the markdown is rendered locally, with no GitHub API call and no token.
 */
class GeneratedHelpArticlesTest {

    private void assertArticleGenerated(String fileName) {
        String resource = "/webapp/pdf-exporter-app/html/" + fileName;
        assertNotNull(getClass().getResource(resource),
                resource + " is missing: markdown2html no longer writes this article into the app webapp, "
                        + "so the page that shows it renders its 'not generated' message");
    }

    @Test
    void aboutArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("about.html");
    }

    @Test
    void disclaimerArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("disclaimer.html");
    }

    // The documentation-site articles of ui/src/docs/docs.config.json, in its reading order.

    @Test
    void quickStartArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("quick-start.html");
    }

    @Test
    void userGuideArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("user-guide.html");
    }

    @Test
    void configurationArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("configuration.html");
    }

    @Test
    void limitationsArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("limitations.html");
    }

    @Test
    void upgradeArticleIsGeneratedIntoTheAppWebapp() {
        assertArticleGenerated("upgrade.html");
    }

}
