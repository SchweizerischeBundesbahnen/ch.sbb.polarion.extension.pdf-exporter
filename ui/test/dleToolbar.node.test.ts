import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, globals, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// dle-toolbar.js is the whole document-editor toolbar integration: the administrator configures one
// dleEditorHead tag, and this script appends react-sbb-polarion's shared engine carrying the button's
// configuration on data-* attributes. The engine installs itself from `document.currentScript`, so
// there is no callback, no queue and no per-extension bootstrap left to test.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/dle-toolbar.js';

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

  /** The engine <script> this injector appends, carrying the button config. */
  const engineScript = (): HTMLScriptElement => {
    const script = document.head.querySelector<HTMLScriptElement>('script[src*="dle-toolbar-starter.js"]');
    if (!script) {
      throw new Error('dle-toolbar.js did not append the engine script');
    }
    return script;
  };

  const load = async (selfUrl = SELF_URL): Promise<void> => {
    setCurrentScript(selfUrl);
    await import('../../src/main/resources/webapp/pdf-exporter/js/dle-toolbar.js');
    await flushPromises();
  };

  it('appends the shared engine from the app webapp, its URL derived from its own', async () => {
    // A non-default web context proves both bases come from document.currentScript rather than being
    // hardcoded to /polarion/pdf-exporter/.
    await load('http://localhost/polarion/my-ctx/js/dle-toolbar.js');

    expect(engineScript().src).toContain('http://localhost/polarion/my-ctx-app/ui/app/dle-toolbar-starter.js');
  });

  it('carries the button configuration on the engine script tag', async () => {
    await load();

    const { dataset } = engineScript();
    // The marker must be the extension's own web context: the engine derives the injected element's
    // id from it, and button ordering across extensions keys off that id.
    expect(dataset.marker).toBe('pdf-exporter');
    expect(dataset.title).toBe('Export to PDF');
    expect(dataset.icon).toContain('actionPdfExport16.svg');
  });

  it('opens the export dialog from the app bundle on click', async () => {
    await load();

    const onclick = engineScript().dataset.onclick!;
    expect(onclick).toContain('/polarion/pdf-exporter-app/ui/app/assets/export-popup.js');
    expect(onclick).toContain("openExportPopup({documentType: 'LIVE_DOC'})");
    expect(onclick).toContain('catch(console.error)');
  });

  it('passes the permission endpoint, which the engine scopes to the project itself', async () => {
    await load();

    // Not project-scoped here: the engine appends the current project when it injects.
    expect(engineScript().dataset.permissionUrl).toBe(
      'http://localhost/polarion/pdf-exporter/rest/internal/permissions/export',
    );
  });

  it('puts nothing else on the page', async () => {
    await load();

    // No stylesheet, no popup library: the button styles are bundled into the engine and the dialog
    // styles itself inside its own shadow root.
    expect(document.head.querySelectorAll('link')).toHaveLength(0);
    expect(document.head.querySelectorAll('script')).toHaveLength(1);
    expect(document.body.innerHTML).toBe('');
    expect(globals().CommonDleToolbarStarter).toBeUndefined();
  });
});
