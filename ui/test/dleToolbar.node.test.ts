import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, globals, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// dle-toolbar.js is the one-tag document-editor injector: it exists so an administrator configures a
// single dleEditorHead script instead of a script plus an inline PdfExporterStarter.injectToolbar call.
// All it does is get starter.js loaded and then make that call.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/dle-toolbar.js';

describe('dle-toolbar.js injector', () => {
  let consoleError: ReturnType<typeof vi.spyOn>;
  let injectToolbarCalls: unknown[];

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    injectToolbarCalls = [];
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  const starterStub = () => ({ injectToolbar: (params?: unknown) => injectToolbarCalls.push(params) });

  /** The starter.js <script> this injector appends when the global is not there yet. */
  const starterScript = (): HTMLScriptElement | null =>
    document.head.querySelector<HTMLScriptElement>('script[src*="starter.js"]');

  const load = async (selfUrl = SELF_URL): Promise<void> => {
    setCurrentScript(selfUrl);
    await import('../../src/main/resources/webapp/pdf-exporter/js/dle-toolbar.js');
    await flushPromises();
  };

  it('reuses an already loaded starter instead of loading it twice', async () => {
    // starter.js may also be configured via mainHead, in which case the global is already there.
    globals().PdfExporterStarter = starterStub();

    await load();

    expect(injectToolbarCalls).toEqual([{ alternate: true }]);
    expect(starterScript()).toBeNull();
  });

  it('loads starter.js from its own URL when the starter is absent', async () => {
    // A non-default web context proves the path is derived from document.currentScript.
    await load('http://localhost/polarion/my-ctx/js/dle-toolbar.js');

    expect(starterScript()?.src).toBe('http://localhost/polarion/my-ctx/js/starter.js');
    expect(injectToolbarCalls).toHaveLength(0); // nothing to call yet
  });

  it('injects the alternate toolbar once the loaded starter.js defines the global', async () => {
    await load();

    globals().PdfExporterStarter = starterStub();
    starterScript()!.dispatchEvent(new window.Event('load'));
    await flushPromises();

    // alternate: true - this button belongs in the toolbar row, not above the editor.
    expect(injectToolbarCalls).toEqual([{ alternate: true }]);
  });

  it('logs instead of throwing when starter.js loads without defining the global', async () => {
    await load();

    starterScript()!.dispatchEvent(new window.Event('load'));
    await flushPromises();

    expect(injectToolbarCalls).toHaveLength(0);
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringContaining('starter.js loaded but PdfExporterStarter is not defined'),
    );
  });

  it('logs when starter.js fails to load', async () => {
    await load();

    starterScript()!.dispatchEvent(new window.Event('error'));
    await flushPromises();

    expect(injectToolbarCalls).toHaveLength(0);
    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('failed to load starter.js'));
  });
});
