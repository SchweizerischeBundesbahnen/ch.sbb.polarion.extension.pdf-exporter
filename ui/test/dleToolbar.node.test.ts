import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// dle-toolbar.js is the one-tag document-editor injector, and since react-sbb-polarion 2.0.0 that is all
// it is: it appends the shared engine with this extension's button on data-* attributes, and the engine
// installs itself from them. Everything it used to carry - the button markup, the project-id parsing, the
// call queue, the ordering - moved into the engine and is covered by the library's own suite.
//
// So what belongs here is the contract between the two: the URL the engine is loaded from, and the
// attributes it is handed.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/dle-toolbar.js';
const APP_BASE = '/polarion/pdf-exporter-app/ui/app/';

describe('dle-toolbar.js injector', () => {
  let consoleError: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  const engineScript = (): HTMLScriptElement | null =>
    document.head.querySelector<HTMLScriptElement>('script[src*="dle-toolbar-starter.js"]');

  const load = async (selfUrl = SELF_URL): Promise<void> => {
    setCurrentScript(selfUrl);
    await import('../../src/main/resources/webapp/pdf-exporter/js/dle-toolbar.js');
    await flushPromises();
  };

  it('loads the engine from the app assets, where the Vite copy step puts it', async () => {
    await load();

    const script = engineScript();
    expect(script).not.toBeNull();
    expect(script!.src).toContain(`${APP_BASE}assets/dle-toolbar-starter.js`);
    // Cache-busted per page load, so an updated extension is picked up on the next page open.
    expect(script!.src).toMatch(/\?timestamp=\d+$/);
  });

  it('describes the button on the script tag, which is what the engine installs from', async () => {
    await load();

    const { dataset } = engineScript()!;
    expect(dataset.marker).toBe('pdf-exporter');
    expect(dataset.title).toBe('Export to PDF');
    expect(dataset.icon).toContain('/polarion/ria/images/dle/operations/actionPdfExport16.svg');
    expect(dataset.permissionUrl).toBe('/polarion/pdf-exporter/rest/internal/permissions/export');
  });

  it('opens the export dialog for a Live Doc on click', async () => {
    await load();

    const onclick = engineScript()!.dataset.onclick!;
    expect(onclick).toContain(`${APP_BASE}assets/export-popup.js`);
    expect(onclick).toContain("documentType: 'LIVE_DOC'");
    // The dialog mounts into a shadow root of its own, so a failure has to reach the console rather
    // than an unhandled rejection.
    expect(onclick).toContain('.catch(console.error)');
  });

  it('logs instead of throwing when the engine cannot be loaded', async () => {
    await load();

    engineScript()!.onerror!(new Event('error'));

    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('failed to load the DLE toolbar engine'));
  });
});
