import { beforeEach, describe, expect, it, vi } from 'vitest';
import { engineRecorder, flushPromises, globals, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// live-reports.js is the recommended report-page injector: a single mainHead script tag that adds the
// "Export to PDF" button to the native Live Report toolbar through the shared self-healing engine.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/live-reports.js';

describe('live-reports.js injector', () => {
  let engine: ReturnType<typeof engineRecorder>;

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    engine = engineRecorder();
  });

  /**
   * Loads live-reports.js with the shared engine already "loaded": the engine element id is present and
   * the global is defined, so loadEngine resolves immediately and the callback runs against the stub.
   */
  const loadInjector = async ({ expandTools = false } = {}): Promise<void> => {
    const engineTag = document.createElement('script');
    engineTag.id = 'generic-dle-toolbar-engine';
    document.head.appendChild(engineTag);
    globals().CommonDleToolbarStarter = engine.stub;
    setCurrentScript(SELF_URL, expandTools ? { expandTools: 'true' } : {});

    await import('../../src/main/resources/webapp/pdf-exporter/js/live-reports.js');
    await flushPromises(); // the engine promise resolves synchronously; let its .then(create) run
  };

  it('injects no stylesheet at all', async () => {
    await loadInjector();

    // The button uses Polarion's own toolbar classes and the export dialog is a React module that
    // styles itself inside its own shadow root. So this extension's own page stylesheet is gone, along
    // with the six generic control stylesheets and the micromodal library the legacy popup needed on
    // the page.
    for (const id of [
      'pdf-exporter-styles',
      'pdf-micromodal-styles',
      'pdf-micromodal-script',
      'generic-control-tokens',
      'generic-checkbox-styles',
      'generic-searchable-dropdown-styles',
      'generic-inputs-styles',
      'generic-alerts-styles',
    ]) {
      expect(document.getElementById(id), id).toBeNull();
    }
    expect(document.querySelectorAll('link[rel="stylesheet"]')).toHaveLength(0);
  });

  it('creates the report-toolbar starter against the richPagePreview target and injects', async () => {
    await loadInjector();
    expect(engine.createdConfigs).toHaveLength(1);
    const config = engine.createdConfigs[0];
    expect(config.markerId).toBe('pdf-exporter-rp-toolbar-injected');
    expect(config.target).toBe('richPagePreview');
    expect(typeof config.order).toBe('number');
    expect(engine.injectToolbarCalls).toHaveLength(1);
  });

  it('builds the button from its own script URL and opens the popup in Live Report context', async () => {
    await loadInjector();
    const html = engine.createdConfigs[0].html;
    expect(html).toContain('/polarion/pdf-exporter-app/ui/app/assets/export-popup.js');
    expect(html).toContain('module.openExportPopup(');
    expect(html).toContain("documentType: 'LIVE_REPORT'");
    expect(html).toContain('Export to PDF');
    // visually separated from the native buttons, like the DLE toolbar button
    expect(html).toContain('toolbar_splitter_gray.gif');
  });

  it('does not auto-expand the tools toolbar by default', async () => {
    await loadInjector();
    expect(engine.calls.autoExpand).toBe(0);
  });

  it('auto-expands the tools toolbar when opted in via data-expand-tools="true"', async () => {
    await loadInjector({ expandTools: true });
    expect(engine.calls.autoExpand).toBe(1);
  });

  it('waits for an in-flight engine load instead of dropping the button (multi-extension)', async () => {
    // Another extension already added the engine <script> (same id) but it hasn't finished loading yet -
    // CommonDleToolbarStarter is not defined. Our onload must NOT run synchronously (that dropped the
    // button before this fix); it must wait for the load event.
    const engineTag = document.createElement('script');
    engineTag.id = 'generic-dle-toolbar-engine';
    document.head.appendChild(engineTag);
    setCurrentScript(SELF_URL);

    await import('../../src/main/resources/webapp/pdf-exporter/js/live-reports.js');
    await flushPromises();

    expect(engine.createdConfigs).toHaveLength(0); // engine not loaded yet -> nothing created

    // The engine finishes loading and defines its global, then fires load.
    globals().CommonDleToolbarStarter = engine.stub;
    engineTag.dispatchEvent(new window.Event('load'));
    await flushPromises(); // the engine promise resolves on load -> its .then(create) runs

    expect(engine.createdConfigs).toHaveLength(1); // button registered after the engine loaded
    expect(engine.injectToolbarCalls).toHaveLength(1);
  });
});
