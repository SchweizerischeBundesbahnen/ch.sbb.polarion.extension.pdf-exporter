import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { engineRecorder, flushPromises, globals, resetInjectorGlobals } from './injectorHarness';

// toolbar-injector.js is what external-js/pdf-exporter-ui-loader.js loads into the main Polarion page.
// It replaces the three scriptInjection injectors and draws only the buttons the `appearance` setting
// of the current project asks for, re-reading that setting whenever the user moves to another project.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const LIVE_DOC_MARKER = 'pdf-exporter-toolbar-injected';
const LIVE_REPORT_MARKER = 'pdf-exporter-rp-toolbar-injected';
const WIKI_MARKER = 'pdf-exporter-wiki-toolbar-injected';

const WIKI_PATH = '/polarion/wiki/bin/view/project/elibrary/page/Wiki';

/** The toolbar row of a wiki page view, reduced to what the script looks for. */
const WIKI_TOOLBAR = `
  <div id="menuView"><table><tbody><tr>
    <td><div id="editButt" class="enab">Edit</div></td>
    <td width="100%">&nbsp;</td>
  </tr></tbody></table></div>`;

/** Everything on, which is what an installation that never opened the Appearance page gets. */
const ALL_ON = {
  liveDocButton: true,
  liveReportButton: true,
  liveReportAutoExpandTools: false,
  classicWikiButton: true,
  classicWikiAutoExpandTools: false,
  permitted: true,
};

function fakeWikiFrame(path = WIKI_PATH) {
  const frame = document.createElement('iframe');
  const showMenubar = vi.fn();
  const doc = document.implementation.createHTMLDocument('wiki');
  doc.body.innerHTML = WIKI_TOOLBAR;
  Object.defineProperty(frame, 'contentWindow', { get: () => ({ location: { pathname: path }, showMenubar }) });
  Object.defineProperty(frame, 'contentDocument', { get: () => doc });
  return { frame, doc, showMenubar };
}

describe('toolbar-injector.js', () => {
  let engine: ReturnType<typeof engineRecorder>;
  let fetchMock: ReturnType<typeof vi.fn>;
  /** The appearance answered per project id; '' is the global scope. */
  let appearances: Record<string, Record<string, boolean>>;
  const observers: MutationObserver[] = [];

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    engine = engineRecorder();
    // The engine is already "loaded": the element id is on the page and the global is defined, so the
    // injector's loadEngine resolves against the stub instead of fetching the real one.
    const engineTag = document.createElement('script');
    engineTag.id = 'common-dle-toolbar-engine';
    document.head.appendChild(engineTag);
    globals().CommonDleToolbarStarter = engine.stub;

    appearances = { '': ALL_ON, elibrary: ALL_ON };
    fetchMock = vi.fn((url: string) => {
      const projectId = new URL(url, 'http://localhost').searchParams.get('projectId') ?? '';
      return Promise.resolve({ ok: true, json: () => Promise.resolve(appearances[projectId]) });
    });
    vi.stubGlobal('fetch', fetchMock);
    // jsdom without pretendToBeVisual has no requestAnimationFrame
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => setTimeout(() => callback(0), 0));
    const Native = window.MutationObserver;
    vi.stubGlobal(
      'MutationObserver',
      class extends Native {
        constructor(callback: MutationCallback) {
          super(callback);
          observers.push(this);
        }
      },
    );
  });

  afterEach(() => {
    observers.splice(0).forEach((observer) => observer.disconnect());
    vi.unstubAllGlobals();
  });

  const load = async (hash = '#/project/elibrary/wiki/Wiki'): Promise<void> => {
    window.location.hash = hash;
    await import('../../src/main/resources/webapp/pdf-exporter/js/toolbar-injector.js');
    await flushPromises();
  };

  const configFor = (markerId: string) => engine.createdConfigs.find((config) => config.markerId === markerId);

  it('reads the appearance of the project in the hash', async () => {
    await load();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe('/polarion/pdf-exporter/rest/internal/appearance?projectId=elibrary');
    expect(fetchMock.mock.calls[0][1]).toEqual({ credentials: 'same-origin' });
  });

  it('asks for the global scope outside a project', async () => {
    await load('#/wiki/Home');

    expect(fetchMock.mock.calls[0][0]).toBe('/polarion/pdf-exporter/rest/internal/appearance');
  });

  it('creates the document editor and Live Report buttons through the shared engine', async () => {
    await load();

    expect(configFor(LIVE_DOC_MARKER)?.target).toBe('dleEditor');
    expect(configFor(LIVE_REPORT_MARKER)?.target).toBe('richPagePreview');
    expect(engine.injectToolbarCalls).toHaveLength(2);
    // The dialog is imported on click, and each toolbar opens it for its own document type.
    expect(configFor(LIVE_DOC_MARKER)?.html).toContain("documentType: 'LIVE_DOC'");
    expect(configFor(LIVE_REPORT_MARKER)?.html).toContain("documentType: 'LIVE_REPORT'");
  });

  it('answers the permission check from the same response the appearance came from', async () => {
    appearances.elibrary = { ...ALL_ON, permitted: false };
    await load();

    // One request, not a second one to the permissions endpoint.
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(configFor(LIVE_DOC_MARKER)?.permissionCheck?.()).toBe(false);
  });

  it('draws no button a project switched off', async () => {
    appearances.elibrary = { ...ALL_ON, liveDocButton: false, liveReportButton: false, classicWikiButton: false };
    const wiki = fakeWikiFrame();
    document.body.appendChild(wiki.frame);
    await load();

    expect(engine.createdConfigs).toHaveLength(0);
    expect(wiki.doc.getElementById(WIKI_MARKER)).toBeNull();
  });

  it('expands the report tools toolbar only when the project asks for it', async () => {
    await load();
    expect(engine.calls.autoExpand).toBe(0);

    vi.resetModules();
    resetInjectorGlobals();
    globals().CommonDleToolbarStarter = engine.stub;
    appearances.elibrary = { ...ALL_ON, liveReportAutoExpandTools: true };
    await load();
    expect(engine.calls.autoExpand).toBe(1);
  });

  it('adds the wiki button to the toolbar inside the wiki iframe', async () => {
    const wiki = fakeWikiFrame();
    document.body.appendChild(wiki.frame);
    await load();

    const cell = wiki.doc.getElementById(WIKI_MARKER);
    expect(cell, 'no button in the wiki toolbar').not.toBeNull();
    expect(cell!.textContent).toContain('Export to PDF');
    // Before the cell that pushes the right-hand buttons to the right
    expect(cell!.nextElementSibling!.getAttribute('width')).toBe('100%');
    // The main page itself gets nothing: no stylesheet, no button of its own.
    expect(document.querySelectorAll('link[rel="stylesheet"], style')).toHaveLength(0);
  });

  it('expands the wiki tools toolbar only when the project asks for it', async () => {
    const wiki = fakeWikiFrame();
    document.body.appendChild(wiki.frame);
    appearances.elibrary = { ...ALL_ON, classicWikiAutoExpandTools: true };
    await load();

    expect(wiki.showMenubar).toHaveBeenCalled();
  });

  it('rebuilds the buttons for the project the user moves to', async () => {
    appearances.other = { ...ALL_ON, liveReportButton: false };
    const wiki = fakeWikiFrame();
    document.body.appendChild(wiki.frame);
    await load();
    expect(wiki.doc.getElementById(WIKI_MARKER)).not.toBeNull();

    // The buttons of the previous project are torn down, including the one inside the wiki iframe,
    // which belongs to a project the user has left.
    appearances.other = { ...appearances.other, classicWikiButton: false };
    window.location.hash = '#/project/other/wiki/Wiki';
    window.dispatchEvent(new window.Event('hashchange'));
    await vi.waitFor(() => expect(engine.destroyed).toContain(LIVE_REPORT_MARKER));

    expect(engine.destroyed).toContain(LIVE_DOC_MARKER);
    expect(wiki.doc.getElementById(WIKI_MARKER)).toBeNull();
    expect(configFor(LIVE_REPORT_MARKER)).toBeDefined(); // from the first project only
    expect(engine.createdConfigs.filter((config) => config.markerId === LIVE_REPORT_MARKER)).toHaveLength(1);
    expect(engine.createdConfigs.filter((config) => config.markerId === LIVE_DOC_MARKER)).toHaveLength(2);
  });

  it('puts the wiki button back after a move to another project which also shows it', async () => {
    const wiki = fakeWikiFrame();
    document.body.appendChild(wiki.frame);
    appearances.other = ALL_ON;
    await load();
    expect(wiki.doc.getElementById(WIKI_MARKER)).not.toBeNull();

    // The iframe is the same element, already watched: the rebuild has to inject into it again rather
    // than wait for a load event which is not coming.
    window.location.hash = '#/project/other/wiki/Wiki';
    window.dispatchEvent(new window.Event('hashchange'));
    await vi.waitFor(() => expect(engine.destroyed).toContain(LIVE_DOC_MARKER));

    expect(wiki.doc.getElementById(WIKI_MARKER)).not.toBeNull();
  });

  it('notices a project change which raised no hashchange event', async () => {
    appearances.other = { ...ALL_ON, liveDocButton: false };
    await load();
    expect(configFor(LIVE_DOC_MARKER)).toBeDefined();

    // Polarion's own navigation does not always raise hashchange, so the DOM observer has to notice.
    window.location.hash = '#/project/other/wiki/Wiki';
    document.body.appendChild(document.createElement('div'));
    await vi.waitFor(() => expect(engine.destroyed).toContain(LIVE_DOC_MARKER));

    expect(engine.createdConfigs.filter((config) => config.markerId === LIVE_DOC_MARKER)).toHaveLength(1);
  });

  it('asks the server once per project', async () => {
    await load();
    window.location.hash = '#/project/elibrary/wiki/Other';
    window.dispatchEvent(new window.Event('hashchange'));
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('keeps the current buttons when the configuration cannot be read', async () => {
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {});
    await load();
    const created = engine.createdConfigs.length;

    fetchMock.mockImplementation(() => Promise.resolve({ ok: false, status: 500 }));
    window.location.hash = '#/project/other/wiki/Wiki';
    window.dispatchEvent(new window.Event('hashchange'));
    await vi.waitFor(() => expect(errors).toHaveBeenCalled());

    expect(engine.createdConfigs).toHaveLength(created);
    expect(engine.destroyed).toHaveLength(0);
    errors.mockRestore();
  });
});
