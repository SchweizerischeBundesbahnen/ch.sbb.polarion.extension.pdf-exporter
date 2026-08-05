/**
 * Where the item being exported lives, read out of the Polarion location hash.
 *
 * This is the TypeScript port of the legacy `ExportContext.js` constructor, which every export surface used
 * to load at runtime from the extension's other webapp. The parsing is not trivial - baselines, collections,
 * test runs opened from a list, the implicit `_default` space, project and repository home pages - and it is
 * the same for all three surfaces (the toolbar popup, the document properties side panel, the bulk export
 * widget), which is why it is one module with tests of its own rather than three readings of the same hash.
 *
 * Everything here is pure but for {@link parseDocumentLocation}'s one DOM read, which is injectable.
 */
import type { DocumentType, ExportType } from '../export/documentType';

export interface DocumentLocation {
  /**
   * What is being exported. Not always what the caller asked for: a hash pointing at a test run makes this
   * `TEST_RUN` whatever the toolbar that opened the dialog assumed.
   */
  documentType: DocumentType;
  /** The project the item belongs to, or null outside any project scope (the global repository). */
  projectId: string | null;
  /** `<space>/<name>`, or undefined for what is not addressed that way (test runs, collections, bulk). */
  locationPath?: string;
  baselineRevision?: string;
  revision?: string;
  /** The hash's own query parameters, which an export has to carry: the renderer reads the item as the page does. */
  urlQueryParameters?: Record<string, string>;
}

/** {@link DocumentLocation} plus what the endpoints want spelled out separately. */
export interface DocumentIdentity extends DocumentLocation {
  /** `project/<id>/` or the empty string - what the settings endpoints call a scope. */
  scope: string;
  spaceId?: string;
  documentName?: string;
}

export interface ParseOptions {
  /** What the surface that opened the dialog assumes it is exporting. Overridden by a test run hash. */
  documentType?: DocumentType;
  /** `BULK` skips the location entirely: the items are picked in the widget, not addressed by the page URL. */
  exportType?: ExportType;
  /**
   * The href of a test run opened from the test runs list, whose own URL does not name it. Injectable
   * because the only way to get it is a Polarion-internal DOM node - see {@link testRunHrefFromDom}.
   */
  resolveTestRunHref?: () => string | null;
}

/**
 * The href of the test run a `.../testruns` page is showing.
 *
 * WARNING: reading it off Polarion's own test run label widget is not a supported API and may stop working;
 * it is, as the legacy code noted, the only way to get this URL. Unlike the legacy code this returns null
 * instead of throwing when the node is absent, which leaves the hash as it was - the same outcome as a
 * server that never rendered the widget.
 */
export function testRunHrefFromDom(): string | null {
  return document.querySelector('.polarion-TestRunLabelWidget-container a')?.getAttribute('href') ?? null;
}

/** The path and the query of a location hash, both unescaped as the legacy parser unescaped them. */
function splitHash(hash: string): { path: string; search?: string } {
  const withoutPrefix = decodeURI(hash.substring(2));
  const separator = withoutPrefix.indexOf('?');
  if (separator < 0) {
    return { path: withoutPrefix };
  }
  return { path: withoutPrefix.slice(0, separator), search: withoutPrefix.slice(separator + 1) };
}

const scopeOf = (path: string): string => {
  const match = /project\/([^/]+)\//.exec(path);
  return match ? `project/${match[1]}/` : '';
};

const projectIdOf = (scope: string): string | null => /project\/(.*)\//.exec(scope)?.[1] ?? null;

const baselineRevisionOf = (path: string): string | undefined => /baseline\/([^/]+)\//.exec(path)?.[1];

/**
 * A location path Polarion left the space out of gets `_default/` back, which is where such a document
 * lives. A test run path is returned untouched - the caller recognizes it by that prefix.
 */
function addDefaultSpaceIfRequired(extractedPath: string | undefined): string {
  if (!extractedPath) {
    return '';
  }
  if (extractedPath.startsWith('testrun') || extractedPath.includes('/')) {
    return extractedPath;
  }
  return `_default/${extractedPath}`;
}

/**
 * `<space>/<name>` of the item the hash addresses, or undefined where the hash addresses none.
 *
 * The project home page and the repository home page are the rich page `_default/Home`, whose URL has no
 * `/wiki/` part - hence the two explicit tests.
 */
function locationPathOf(path: string, scope: string): string | undefined {
  if (scope) {
    // Greedy on purpose: a document inside a collection has two path segments before its own `/wiki/`.
    const match = /project\/(.+)\/(wiki\/([^?#]+)|testruns|testrun)/.exec(path);
    if (match) {
      return addDefaultSpaceIfRequired(match[3] || match[2]);
    }
    return /project\/[^/]+\/home$/.test(path) ? '_default/Home' : undefined;
  }
  const globalMatch = /wiki\/([^?#]+)/.exec(path);
  if (globalMatch) {
    return addDefaultSpaceIfRequired(globalMatch[1]);
  }
  return path === 'home' ? '_default/Home' : undefined;
}

const queryParametersOf = (search: string | undefined): Record<string, string> | undefined =>
  search === undefined ? undefined : Object.fromEntries(new URLSearchParams(search));

/** Where the item lives, from a Polarion location hash such as `#/project/elibrary/wiki/Specs/Doc`. */
export function parseDocumentLocation(hash: string, options: ParseOptions = {}): DocumentLocation {
  const { documentType = 'LIVE_DOC', exportType = 'SINGLE', resolveTestRunHref = testRunHrefFromDom } = options;

  // A test run opened from the list is on a `.../testruns` URL that does not say which one, so the run's
  // own href is fetched from the page and parsed instead.
  const effectiveHash = hash.endsWith('/testruns') ? (resolveTestRunHref() ?? hash) : hash;

  const { path, search } = splitHash(effectiveHash);
  const scope = scopeOf(path);
  const urlQueryParameters = queryParametersOf(search);

  const location: DocumentLocation = {
    documentType,
    projectId: projectIdOf(scope),
    baselineRevision: baselineRevisionOf(path),
    urlQueryParameters,
    revision: urlQueryParameters?.revision,
  };

  if (exportType === 'BULK') {
    return location;
  }

  const locationPath = locationPathOf(path, scope);
  if (locationPath?.startsWith('testrun')) {
    // A test run is addressed by its id in the query, not by a path, whatever the caller assumed.
    return { ...location, documentType: 'TEST_RUN' };
  }
  return { ...location, locationPath };
}

const pathParts = (locationPath: string | undefined): string[] | undefined =>
  locationPath?.includes('/') ? locationPath.split('/') : undefined;

export const spaceIdOf = (location: DocumentLocation): string | undefined => pathParts(location.locationPath)?.[0];

export const documentNameOf = (location: DocumentLocation): string | undefined => pathParts(location.locationPath)?.[1];

/** The scope the settings endpoints take: `project/<id>/`, or the empty string for the global scope. */
export const scopeFor = (location: DocumentLocation): string =>
  location.projectId ? `project/${location.projectId}/` : '';

/** A location with everything the endpoints ask for spelled out. */
export function toDocumentIdentity(location: DocumentLocation): DocumentIdentity {
  return {
    ...location,
    scope: scopeFor(location),
    spaceId: spaceIdOf(location),
    documentName: documentNameOf(location),
  };
}

/** Where the current page is, for a surface that has no location of its own to point at. */
export const currentDocumentLocation = (options: ParseOptions = {}): DocumentLocation =>
  parseDocumentLocation(window.location.hash, options);
