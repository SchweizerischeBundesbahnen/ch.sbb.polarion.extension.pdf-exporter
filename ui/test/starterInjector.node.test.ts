import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { engineRecorder, flushPromises, globals, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// starter.js is the deprecated-but-supported document-editor entry point: dleEditorHead loads it and
// calls PdfExporterStarter.injectToolbar({...}). It exposes that global synchronously, pulls generic's
// shared toolbar engine, and replays whatever was queued in between.
//
// The import specifier must be written out in full at every call site - see the note in
// injectorHarness.ts. The file lives outside the Vite root, and a specifier held in a variable is
// resolved against the root instead of against this file.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/starter.js';

describe('starter.js injector', () => {
  let consoleError: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  /** The engine <script> starter.js appends to the page head. */
  const engineScript = (): HTMLScriptElement => {
    const script = document.head.querySelector<HTMLScriptElement>('script[src*="dle-toolbar-starter.js"]');
    if (!script) {
      throw new Error('starter.js did not append the engine script');
    }
    return script;
  };

  const load = async (selfUrl = SELF_URL): Promise<void> => {
    setCurrentScript(selfUrl);
    await import('../../src/main/resources/webapp/pdf-exporter/js/starter.js');
    await flushPromises();
  };

  const starter = (): { injectToolbar: (params?: unknown) => void } =>
    globals().PdfExporterStarter as { injectToolbar: (params?: unknown) => void };

  it('exposes PdfExporterStarter before the engine has loaded', async () => {
    await load();

    // The dleEditorHead config calls injectToolbar in the same synchronous pass that loads this script,
    // so the global cannot wait for the engine.
    expect(starter()).toBeDefined();
    expect(typeof starter().injectToolbar).toBe('function');
  });

  it('appends the shared engine script, its URL derived from its own', async () => {
    // A non-default web context proves the base is read from document.currentScript rather than
    // hardcoded to /polarion/pdf-exporter/.
    await load('http://localhost/polarion/my-ctx/js/starter.js');

    expect(engineScript().src).toContain('http://localhost/polarion/my-ctx/ui/generic/js/dle-toolbar-starter.js');
  });

  it('queues injectToolbar calls made before the engine loads, then replays them in order', async () => {
    const engine = engineRecorder();
    await load();

    starter().injectToolbar({ alternate: true });
    starter().injectToolbar({ alternate: false });
    expect(engine.injectToolbarCalls).toHaveLength(0); // nothing to inject into yet

    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(engine.createdConfigs).toHaveLength(1);
    expect(engine.injectToolbarCalls).toEqual([{ alternate: true }, { alternate: false }]);
  });

  it('passes injectToolbar straight through once the engine is loaded', async () => {
    const engine = engineRecorder();
    await load();
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    starter().injectToolbar({ alternate: true });

    expect(engine.injectToolbarCalls).toEqual([{ alternate: true }]);
  });

  it('hands both toolbar layouts and the LIVE_DOC popup call to the engine', async () => {
    const engine = engineRecorder();
    await load();
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    const config = engine.createdConfigs[0];
    expect(config.markerId).toBe('pdf-exporter-toolbar-injected');
    for (const html of [config.defaultHtml, config.alternateHtml]) {
      // A stringly-typed contract with the React module: no compile-time link ties these together.
      expect(html).toContain('/polarion/pdf-exporter-app/ui/app/assets/export-popup.js');
      expect(html).toContain('module.openExportPopup(');
      expect(html).toContain("documentType: 'LIVE_DOC'");
    }
    // Only the alternate layout sits inside the toolbar row, between two native separators.
    expect(config.alternateHtml).toContain('toolbar_splitter_gray.gif');
    expect(config.defaultHtml).not.toContain('toolbar_splitter_gray.gif');
  });

  it('builds the permission URL with the project id read from the location hash', async () => {
    const engine = engineRecorder();
    window.location.hash = '#/project/elibrary/wiki/Documents';
    await load();
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(engine.createdConfigs[0].permissionCheckUrl).toBe(
      'http://localhost/polarion/pdf-exporter/rest/internal/permissions/export?projectId=elibrary',
    );
  });

  it('URL-encodes a project id that needs it', async () => {
    const engine = engineRecorder();
    // The hash is decoded before the id is matched, so an id with a space arrives as one token and has
    // to be re-encoded for the query string.
    window.location.hash = '#/project/my%20project/wiki';
    await load();
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(engine.createdConfigs[0].permissionCheckUrl).toContain('?projectId=my%20project');
  });

  it('omits the project id when the hash carries no project scope', async () => {
    const engine = engineRecorder();
    window.location.hash = '#/dashboard';
    await load();
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    // Only the global roles apply then; the endpoint takes no projectId.
    expect(engine.createdConfigs[0].permissionCheckUrl).toBe(
      'http://localhost/polarion/pdf-exporter/rest/internal/permissions/export',
    );
  });

  it('takes its order from the shared cross-extension registry', async () => {
    const engine = engineRecorder();
    // Two other extensions registered first, so this button keeps its place on a re-render.
    globals().__genericDleToolbarSeq = { n: 2 };
    await load();

    starter().injectToolbar({ alternate: true });
    globals().GenericDleToolbarStarter = engine.stub;
    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(engine.createdConfigs[0].order).toBe(2);
    expect(globals().__genericDleToolbarSeq).toEqual({ n: 3 });
  });

  it('logs and drops the queue when the engine loads without defining its global', async () => {
    await load();
    starter().injectToolbar({ alternate: true });

    engineScript().dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('GenericDleToolbarStarter is not available'));
  });

  it('logs when the engine script fails to load', async () => {
    await load();
    starter().injectToolbar({ alternate: true });

    engineScript().dispatchEvent(new window.Event('error'));
    await flushPromises();

    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('failed to load the DLE toolbar engine'));
  });
});
