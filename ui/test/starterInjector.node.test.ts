import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, globals, resetInjectorGlobals, setCurrentScript } from './injectorHarness';

// starter.js is a backward-compatibility shim: an older administrator configuration loads it and calls
// PdfExporterStarter.injectToolbar(), and that has to keep working. Since react-sbb-polarion 2.0.0 it does
// so by loading dle-toolbar.js, which is where the button is actually described - the engine's own
// behaviour (ordering, permission check, self-healing) is covered by the library's suite, and the earlier
// cases here that asserted it through this file went with the code that did it.
//
// The import specifier must be written out in full at every call site - see injectorHarness.ts.

const SELF_URL = 'http://localhost/polarion/pdf-exporter/js/starter.js';

describe('starter.js compatibility shim', () => {
  let consoleError: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.resetModules();
    resetInjectorGlobals();
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleError.mockRestore();
  });

  const injectorScript = (): HTMLScriptElement | null =>
    document.head.querySelector<HTMLScriptElement>('script[src*="dle-toolbar.js"]');

  const load = async (selfUrl = SELF_URL): Promise<void> => {
    setCurrentScript(selfUrl);
    await import('../../src/main/resources/webapp/pdf-exporter/js/starter.js');
    await flushPromises();
  };

  const starter = () => globals().PdfExporterStarter as { injectToolbar: (params?: unknown) => void };

  it('exposes the global the old configuration calls', async () => {
    await load();

    expect(typeof starter().injectToolbar).toBe('function');
    // Nothing happens until that call: loading this script alone must not put a button anywhere.
    expect(injectorScript()).toBeNull();
  });

  it('injects through dle-toolbar.js, resolved from its own URL', async () => {
    await load();
    starter().injectToolbar();
    await flushPromises();

    expect(injectorScript()!.src).toContain('/polarion/pdf-exporter/js/dle-toolbar.js');
  });

  it('accepts the alternate flag of the removed second placement, and ignores it', async () => {
    await load();
    starter().injectToolbar({ alternate: true });
    await flushPromises();

    // One button, one placement: the flag used to choose the floating variant, which no longer exists.
    expect(document.head.querySelectorAll('script[src*="dle-toolbar.js"]')).toHaveLength(1);
  });

  it('loads the injector once however often it is called', async () => {
    await load();
    starter().injectToolbar();
    starter().injectToolbar({ alternate: true });
    await flushPromises();

    expect(document.head.querySelectorAll('script[src*="dle-toolbar.js"]')).toHaveLength(1);
  });

  it('logs instead of throwing when the injector cannot be loaded', async () => {
    await load();
    starter().injectToolbar();
    injectorScript()!.onerror!(new Event('error'));

    expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('failed to load dle-toolbar.js'));
  });
});
