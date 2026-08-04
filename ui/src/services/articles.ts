/** Where this extension's sources live; used when a build-generated article is missing. */
export const PROJECT_URL = 'https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter';

/**
 * The build-generated DISCLAIMER article, served as a static file by `PdfExporterAppServlet` from
 * this extension's app webapp (markdown2html writes it there, next to about.html and
 * user-guide.html). About and User Guide come from generic's REST endpoints instead; there is no
 * endpoint for the disclaimer.
 */
export const DISCLAIMER_URL = '/polarion/pdf-exporter-app/ui/html/disclaimer.html';

/**
 * Fetches a generated help article. Returns `null` when it is absent - the servlet answers 404 for a
 * file that was never generated, and Polarion answers with its own HTML page for a path it does not
 * recognise, so an empty body counts as absent too.
 */
export async function fetchArticle(url: string): Promise<string | null> {
  const response = await fetch(url, { method: 'GET', cache: 'no-cache' });
  if (!response.ok) {
    return null;
  }
  const text = await response.text();
  return text.trim() === '' ? null : text;
}
