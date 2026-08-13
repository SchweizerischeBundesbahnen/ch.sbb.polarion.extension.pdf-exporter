/**
 * Fetches a project's documents from the standard Polarion platform REST API
 * (`GET /polarion/rest/v1/projects/{projectId}/documents`, JSON:API shape).
 *
 * Used only by the side panel's development harness, to let a developer pick a real document to feed the
 * panel: `vite dev` has no Polarion editor around it, so there is no document to be "in". In dev the
 * request is proxied to the configured Polarion and authenticated with `VITE_BEARER_TOKEN`.
 *
 * Each document carries `attributes.moduleFolder` (the space id) and `attributes.moduleName` (the document
 * name) - the two values a Polarion editor URL is built from.
 */

export interface ProjectDocument {
  spaceId: string;
  moduleName: string;
}

export interface DocumentList {
  documents: ProjectDocument[];
  /** True when the page cap was reached, so the list is not the whole project. */
  truncated: boolean;
}

const PAGE_SIZE = 100;

/**
 * How many pages are followed at most. A project with more documents than this is a project nobody picks
 * a single document out of a dropdown in, and an unbounded loop against a live server is not something a
 * development page should do.
 */
export const MAX_PAGES = 20;

const firstPageUrl = (projectId: string): string =>
  `/polarion/rest/v1/projects/${encodeURIComponent(projectId)}/documents` +
  `?page%5Bsize%5D=${PAGE_SIZE}&fields%5Bdocuments%5D=moduleName%2CmoduleFolder`;

/**
 * The next page as a path, so it keeps going through the `vite dev` proxy.
 *
 * Polarion answers with an absolute `links.next`, which points at the server directly - fetched as it is,
 * the browser would leave the dev origin and be refused by CORS. Only the path and query are kept, which
 * is the same URL as far as the proxy is concerned.
 */
export function nextPagePath(next: unknown): string | null {
  if (typeof next !== 'string' || next === '') {
    return null;
  }
  try {
    const url = new URL(next, window.location.origin);
    return `${url.pathname}${url.search}`;
  } catch {
    return null;
  }
}

/** A document is only usable here when it has both halves of an editor URL. */
const toDocuments = (data: unknown): ProjectDocument[] =>
  (Array.isArray(data) ? data : [])
    .map((item) => {
      const attributes = (item as { attributes?: { moduleFolder?: string; moduleName?: string } })?.attributes;
      return { spaceId: attributes?.moduleFolder ?? '', moduleName: attributes?.moduleName ?? '' };
    })
    .filter((document) => document.spaceId && document.moduleName);

export const documentPath = (document: ProjectDocument): string => `${document.spaceId}/${document.moduleName}`;

/** The space Polarion leaves out of a document's URL, and which the export context puts back. */
const DEFAULT_SPACE = '_default';

/**
 * The Polarion editor hash a document is opened at, e.g. `#/project/elibrary/wiki/Specs/Cross%20Link%20Issue`.
 *
 * This is what lets the development harness drive the **real** panel: the product's `ExportContext` reads
 * `window.location.hash` to work out which document is being exported, so writing the hash a real editor
 * would have is what makes the harness a real scenario rather than a stub. Two details of that parser are
 * mirrored here:
 *
 * - A document in `_default` is addressed without its space, exactly as Polarion addresses it; the parser
 *   prepends `_default/` again (`addDefaultSpaceIfRequired`). Emitting it in full would work too, but then
 *   the harness would never exercise that branch.
 * - Segments are escaped with `encodeURI`, because the parser unescapes with `decodeURI`. The pair is
 *   exact for spaces and the other characters Polarion puts in a URL; a document name containing `?` or
 *   `#` is beyond what either side handles, in Polarion as much as here.
 */
export function documentEditorHash(projectId: string, document: ProjectDocument): string {
  const path =
    document.spaceId === DEFAULT_SPACE
      ? encodeURI(document.moduleName)
      : `${encodeURI(document.spaceId)}/${encodeURI(document.moduleName)}`;
  return `#/project/${encodeURI(projectId)}/wiki/${path}`;
}

/** Every document of the project, `<space>/<name>` order, following the pages up to {@link MAX_PAGES}. */
export async function fetchDocuments(projectId: string): Promise<DocumentList> {
  const token = import.meta.env.VITE_BEARER_TOKEN;
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Keyed by path, so a document seen twice is listed once. The pages are separate requests, so a document
  // added or removed between them can shift the window and repeat an entry - and a repeated entry means two
  // identical options in the picker, which React rejects as a duplicate key and a user cannot tell apart.
  const found = new Map<string, ProjectDocument>();
  let url: string | null = firstPageUrl(projectId);
  let pages = 0;

  while (url && pages < MAX_PAGES) {
    const response: Response = await fetch(url, { headers, cache: 'no-cache' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const body = (await response.json()) as { data?: unknown; links?: { next?: unknown } };
    for (const document of toDocuments(body?.data)) {
      found.set(documentPath(document), document);
    }
    url = nextPagePath(body?.links?.next);
    pages++;
  }

  const documents = [...found.values()].sort((a, b) => documentPath(a).localeCompare(documentPath(b)));
  return { documents, truncated: url !== null };
}
