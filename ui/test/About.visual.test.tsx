import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture } from './visualHelpers';

// Docker-only full-page snapshot of the About page (shared RSP About component fed this app's
// endpoints, mocked). Covers the extension-info / properties / status tables and the README article.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('About page visual', () => {
  it('loaded (info + properties + status tables, README article)', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/version$/,
        json: {
          bundleName: 'PDF Exporter',
          bundleVendor: 'SBB',
          supportEmail: 'polarion-opensource@sbb.ch',
          automaticModuleName: 'ch.sbb.polarion.extension.pdf_exporter',
          bundleVersion: '1.0.0',
          bundleBuildTimestamp: '2026-07-01 10:00',
        },
      },
      {
        method: 'GET',
        match: /\/configuration-properties$/,
        json: {
          properties: [
            {
              key: 'ch.sbb.polarion.extension.pdf-exporter.some.property',
              value: 'value',
              defaultValue: 'value',
              description: 'An example configuration property',
            },
          ],
          obsoleteProperties: [],
        },
      },
      {
        method: 'GET',
        match: /\/configuration-status/,
        json: [{ name: 'PDF Exporter', status: 'OK', details: 'ready' }],
      },
      {
        method: 'GET',
        match: /\/readme$/,
        respond: () =>
          new Response(
            '<h1>PDF Exporter Extension for Polarion ALM</h1><p>Exports Polarion documents and work items to PDF.</p>',
            { status: 200 },
          ),
      },
    ]);
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await settleBeforeCapture();
    await expect(page.elementLocator(app)).toMatchScreenshot('about-loaded');
  });
});
