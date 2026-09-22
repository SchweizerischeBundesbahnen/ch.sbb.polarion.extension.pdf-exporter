import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// classic-wiki.js adds the "Export to PDF" button to the toolbar of a Classic Wiki page. Polarion renders that
// page into a same-origin iframe which no scriptInjection property reaches, so the script runs in the
// main page and injects into the iframe from outside.
//
// jsdom gives every iframe an about:blank document whose location cannot be redefined, so each test
// iframe carries a stand-in document and window instead. The script reads the path from the window.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/classic-wiki.js';
const MARKER_ID = 'pdf-exporter-wiki-toolbar-injected';
const WIKI_PATH = '/polarion/wiki/bin/view/project/elibrary/page/Wiki';

// The toolbar row of a wiki page view, reduced to what the script looks for: the Edit button and the
// cell that pushes the right-hand buttons to the right.
const TOOLBAR = `
  <div id="panelSwitcher"><span class="handle">Expand Tools</span></div>
  <div id="menuView"><table><tbody><tr>
    <td><div id="editButt" class="enab">Edit</div></td>
    <td><div id="formaction_extract" class="disab">Extract Work Item</div></td>
    <td width="100%">&nbsp;</td>
    <td><div class="enab" title="History">History</div></td>
  </tr></tbody></table></div>`;

interface FakeFrame {
  frame: HTMLIFrameElement;
  doc: Document;
  showMenubar: ReturnType<typeof vi.fn>;
  /** Swaps in a freshly loaded page, the way Polarion reloads the iframe on wiki navigation. */
  reload: (path?: string, body?: string) => void;
}

function fakeFrame(path = WIKI_PATH, body = TOOLBAR): FakeFrame {
  const frame = document.createElement('iframe');
  const showMenubar = vi.fn();
  const win = { location: { pathname: path }, showMenubar };
  let doc = document.implementation.createHTMLDocument('wiki');
  doc.body.innerHTML = body;
  Object.defineProperty(frame, 'contentWindow', { get: () => win, configurable: true });
  Object.defineProperty(frame, 'contentDocument', { get: () => doc, configurable: true });
  const result: FakeFrame = {
    frame,
    get doc() {
      return doc;
    },
    showMenubar,
    reload: (nextPath = WIKI_PATH, nextBody = TOOLBAR) => {
      win.location.pathname = nextPath;
      doc = document.implementation.createHTMLDocument('wiki');
      doc.body.innerHTML = nextBody;
      frame.dispatchEvent(new window.Event('load'));
    },
  };
  return result;
}

describe('classic-wiki.js injector', () => {
  // Each test evaluates the script again, and each evaluation starts an observer on the shared jsdom
  // document. They are disconnected after every test so that one test's observer cannot claim the next
  // test's iframes.
  const observers: MutationObserver[] = [];

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    window.location.hash = '#/project/elibrary/wiki/Wiki';
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

  const loadInjector = async ({ expandTools = false } = {}): Promise<void> => {
    setCurrentScript(SELF_URL, expandTools ? { expandTools: 'true' } : {});
    await import('../../src/main/resources/webapp/pdf-exporter/js/classic-wiki.js');
    await flushPromises();
  };

  const injected = (fake: FakeFrame) => fake.doc.getElementById(MARKER_ID);

  it('adds the button to the toolbar of a wiki page already on the page', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    const cell = injected(fake);
    expect(cell, 'no button in the wiki toolbar').not.toBeNull();
    expect(cell!.textContent).toContain('Export to PDF');
    // The wiki's own separator first, as on the Live Report toolbar, then the icon
    expect([...cell!.querySelectorAll('img')].map((image) => image.getAttribute('src'))).toEqual([
      '/polarion/wiki/skins/sidecar/separatorbig.gif',
      '/polarion/ria/images/dle/operations/actionPdfExport16.svg',
    ]);
    // After the native left-hand buttons, before the spacer
    expect(cell!.previousElementSibling!.querySelector('#formaction_extract')).not.toBeNull();
    expect(cell!.nextElementSibling!.getAttribute('width')).toBe('100%');
  });

  it('injects no stylesheet and puts nothing on the main page', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(document.querySelectorAll('link[rel="stylesheet"], style')).toHaveLength(0);
    expect(document.getElementById(MARKER_ID)).toBeNull();
  });

  it('adds the button to a wiki iframe that appears later', async () => {
    await loadInjector();
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);

    // The observer defers its scan to the next frame
    await vi.waitFor(() => expect(injected(fake)).not.toBeNull());
  });

  it('adds the button again after the iframe loads the next wiki page', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    fake.reload();
    expect(injected(fake)).not.toBeNull();
  });

  it('adds the button once however often the page is scanned', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    // Any mutation triggers another scan, which the observer defers to the next frame
    document.body.appendChild(document.createElement('div'));
    await new Promise((resolve) => setTimeout(resolve, 10));
    fake.frame.dispatchEvent(new window.Event('load'));

    expect(fake.doc.querySelectorAll(`#${MARKER_ID}`)).toHaveLength(1);
  });

  it('leaves an iframe alone that is not a wiki page view', async () => {
    const fake = fakeFrame('/polarion/ria/prefetch.jsp');
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(injected(fake)).toBeNull();
  });

  it.each([
    ['a plan', '#/project/elibrary/plan?id=Iteration_0'],
    ['a test run', '#/project/elibrary/testrun?id=run_1'],
    ['a plan of a project named wiki', '#/project/wiki/plan?id=Iteration_0'],
  ])('leaves the toolbar of %s alone, whose hash addresses no wiki page', async (_, hash) => {
    window.location.hash = hash;
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(injected(fake)).toBeNull();
  });

  it.each([
    ['a global wiki page', '#/wiki/Wiki'],
    ['a wiki page in a space', '#/project/elibrary/wiki/Specification/Home'],
    ['a wiki page in a baseline', '#/baseline/1/project/elibrary/wiki/Wiki'],
  ])('adds the button to %s', async (_, hash) => {
    window.location.hash = hash;
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(injected(fake)).not.toBeNull();
  });

  it('leaves a wiki view without the toolbar alone', async () => {
    const fake = fakeFrame(WIKI_PATH, '<div>no toolbar here</div>');
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(injected(fake)).toBeNull();
  });

  it('dims the icon and shows it in full on hover, like the Live Report toolbar', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();
    const button = injected(fake)!.querySelector<HTMLElement>('[role="button"]')!;
    const icon = button.querySelector<HTMLImageElement>('.bt-icon img')!;

    expect(icon.style.opacity).toBe('0.6');
    button.dispatchEvent(new window.MouseEvent('mouseenter'));
    expect(icon.style.opacity).toBe('1');
    button.dispatchEvent(new window.MouseEvent('mouseleave'));
    expect(icon.style.opacity).toBe('0.6');
  });

  it('opens the export dialog for a wiki page on click and on Enter', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();
    const button = injected(fake)!.querySelector<HTMLElement>('[role="button"]')!;
    // The popup module is a Vite build output that does not exist in this project; its import fails
    // and the failure is logged. What matters is that each activation asks for it once.
    const logged = vi.spyOn(console, 'error').mockImplementation(() => {});

    button.click();
    button.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Enter' }));
    button.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'a' }));
    await vi.waitFor(() => expect(logged).toHaveBeenCalledTimes(2));
  });

  it('does not expand the toolbar by default', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector();

    expect(fake.showMenubar).not.toHaveBeenCalled();
  });

  it('expands the toolbar on every wiki page when opted in via data-expand-tools="true"', async () => {
    const fake = fakeFrame();
    document.body.appendChild(fake.frame);
    await loadInjector({ expandTools: true });
    expect(fake.showMenubar).toHaveBeenCalledTimes(1);

    fake.reload();
    expect(fake.showMenubar).toHaveBeenCalledTimes(2);
  });
});
