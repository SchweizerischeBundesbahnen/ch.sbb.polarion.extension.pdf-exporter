import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

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
        // Rows as the status providers write them, with a warning and an error for their colors
        json: [
          {
            name: 'Bulk Processing Service',
            status: 'ERROR',
            details: 'java.net.UnknownHostException: bulk-processing.local',
          },
          { name: 'Polarion ALM', status: 'OK', details: '2606' },
          { name: 'WeasyPrint Service', status: 'OK', details: '70.0.7 (2026-10-24T20:45:49Z)' },
          { name: 'WeasyPrint Service: Python', status: 'OK', details: '3.15.1' },
          { name: 'WeasyPrint Service: SVG renderer', status: 'OK', details: 'weasysvg 0.3.1' },
          {
            name: 'WeasyPrint Service: Visio drawings',
            status: 'WARNING',
            details:
              "<span style='color: orange;'>not available: Visio drawings are left out of the PDF. weasyprint-service draws them from 70.0.7 on, with libvisio-tools in its image</span>",
          },
          { name: 'WeasyPrint Service: WeasyPrint', status: 'OK', details: '70.0' },
        ],
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
    // The configuration status loads apart from the rest of the page
    await vi.waitFor(() =>
      expect(
        Array.from(document.querySelectorAll('td')).some((td) => td.textContent === 'WeasyPrint Service: WeasyPrint'),
      ).toBe(true),
    );
    const app = document.querySelector('.app') as HTMLElement;
    await settleLayout();
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await settleBeforeCapture();
    await expect(page.elementLocator(app)).toMatchScreenshot('about-loaded');
  });
});
