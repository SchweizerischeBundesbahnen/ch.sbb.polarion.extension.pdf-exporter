// Shared setup for the *.node.test.ts suites that cover the product injector scripts in
// src/main/resources/webapp/pdf-exporter/js/ (dle-toolbar.js, live-reports.js, starter.js).
//
// Those are plain IIFEs, not modules: they read `document.currentScript` and `top` at load time and
// leave their state on `top`. So every test needs the globals in place BEFORE the import, a fresh
// evaluation (vi.resetModules() plus a dynamic import), and a clean `top` afterwards - jsdom does not
// recreate the window between test files in the same project.
//
// In jsdom top === window, which is what production looks like: these scripts are loaded through
// Polarion's scriptInjection into the main page, not into an editor iframe. See vitest.config.ts for
// why they are not tested in browser mode.

export type Globals = Record<string, unknown>;

/** `top` as a writable bag, which is how the injectors treat it. */
export const globals = (): Globals => window as unknown as Globals;

/** Lets a promise chain settle. The engine callbacks resolve synchronously but run in a `.then()`. */
export const flushPromises = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

/**
 * The <script> tag the Polarion scriptInjection config would produce. Not appended: the injectors read
 * `document.currentScript` to derive their extension base URL, and appending it would also show up in
 * the assertions that count what a script put on the page.
 */
export const setCurrentScript = (src: string, dataset: Record<string, string> = {}): HTMLScriptElement => {
  const tag = document.createElement('script');
  tag.src = src;
  Object.assign(tag.dataset, dataset);
  Object.defineProperty(document, 'currentScript', { value: tag, configurable: true });
  return tag;
};

/** Everything an injector leaves behind, plus the location hash they read the project id from. */
export const resetInjectorGlobals = (): void => {
  document.head.innerHTML = '';
  document.body.innerHTML = '';
  delete globals().PdfExporterStarter;
  delete globals().CommonDleToolbarStarter;
  delete globals().__genericDleToolbarEnginePromise;
  delete globals().__genericDleToolbarSeq;
  window.location.hash = '';
};

/** What an injector hands to the shared engine's `create`. */
export interface ToolbarConfig {
  markerId: string;
  html?: string;
  target?: string;
  order?: number;
  permissionCheckUrl?: string;
}

/**
 * A stand-in for the library's CommonDleToolbarStarter that records what it was asked to do. The arrays
 * stay live, so a test can assert on them after the import.
 */
export const engineRecorder = () => {
  const createdConfigs: ToolbarConfig[] = [];
  const injectToolbarCalls: unknown[] = [];
  const calls = { autoExpand: 0 };
  const stub = {
    create: (config: ToolbarConfig) => {
      createdConfigs.push(config);
      return { injectToolbar: (params?: unknown) => injectToolbarCalls.push(params) };
    },
    autoExpandRichPageTools: () => {
      calls.autoExpand++;
    },
  };
  return { createdConfigs, injectToolbarCalls, calls, stub };
};
