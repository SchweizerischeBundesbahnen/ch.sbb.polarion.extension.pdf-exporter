// Shared setup for the *.node.test.ts suites that cover the product injector script
// src/main/resources/webapp/pdf-exporter/js/toolbar-injector.js.
//
// It is a plain IIFE, not a module: it reads `top` at load time and leaves its state on `top`. So a
// test needs the globals in place BEFORE the import, a fresh evaluation (vi.resetModules() plus a
// dynamic import), and a clean `top` afterwards - jsdom does not recreate the window between test
// files in the same project.
//
// In jsdom top === window, which is what production looks like: Polarion loads the script into the
// main page, not into an editor iframe. See vitest.config.ts for why it is not tested in browser mode.

export type Globals = Record<string, unknown>;

/** `top` as a writable bag, which is how the injector treats it. */
export const globals = (): Globals => window as unknown as Globals;

/** Lets a promise chain settle. The engine callbacks resolve synchronously but run in a `.then()`. */
export const flushPromises = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

/** Everything an injector leaves behind, plus the location hash they read the project id from. */
export const resetInjectorGlobals = (): void => {
  document.head.innerHTML = '';
  document.body.innerHTML = '';
  delete globals().CommonDleToolbarStarter;
  delete globals().__commonDleToolbarEnginePromise;
  delete globals().__genericDleToolbarSeq;
  delete globals().__pdfExporterToolbarInstall;
  window.location.hash = '';
};

/** What an injector hands to the shared engine's `create`. */
export interface ToolbarConfig {
  markerId: string;
  html?: string;
  target?: string;
  order?: number;
  permissionCheckUrl?: string;
  permissionCheck?: () => boolean | Promise<boolean>;
}

/**
 * A stand-in for the library's CommonDleToolbarStarter that records what it was asked to do. The arrays
 * stay live, so a test can assert on them after the import.
 */
export const engineRecorder = () => {
  const createdConfigs: ToolbarConfig[] = [];
  const injectToolbarCalls: unknown[] = [];
  const destroyed: string[] = [];
  const calls = { autoExpand: 0 };
  const stub = {
    create: (config: ToolbarConfig) => {
      createdConfigs.push(config);
      return {
        injectToolbar: (params?: unknown) => injectToolbarCalls.push(params),
        destroy: () => destroyed.push(config.markerId),
      };
    },
    // Enough of the real markup for an assertion on where the click goes; the engine's own copy is
    // covered by its own tests in react-sbb-polarion.
    buildButtonHtml: (config: { title: string; iconUrl: string; onClick: string }) =>
      `<div role="button" title="${config.title}" onclick="${config.onClick}"><img src="${config.iconUrl}"></div>`,
    autoExpandRichPageTools: () => {
      calls.autoExpand++;
    },
  };
  return { createdConfigs, injectToolbarCalls, destroyed, calls, stub };
};
