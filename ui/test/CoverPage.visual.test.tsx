import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { filled, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the cover page: the HTML and CSS editors side by side, and the predefined
// templates pane below them - the part of this page no other page has.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-cover-page=; path=/; max-age=0';
});

describe.skipIf(!__PIXEL_REFERENCES__)('Cover page visual', () => {
  it('custom template with the predefined templates offered below', async () => {
    await snapshotFeature(
      'cover-page',
      [
        {
          method: 'GET',
          match: /\/settings\/cover-page\/names\?/,
          json: [{ name: 'Default', scope: 'project/elibrary/' }],
        },
        {
          method: 'GET',
          match: /\/settings\/cover-page\/names\/[^/]+\/content/,
          json: {
            useCustomValues: true,
            templateHtml: '<div class="cover">\n  <h1>$documentTitle</h1>\n  <p>$revision</p>\n</div>',
            templateCss: '.cover h1 {\n  font-size: 32pt;\n}',
          },
        },
        {
          method: 'GET',
          match: /\/settings\/cover-page\/default-content/,
          json: { useCustomValues: false, templateHtml: '', templateCss: '' },
        },
        { method: 'GET', match: /\/settings\/cover-page\/templates$/, json: ['Corporate', 'Minimal'] },
        { method: 'GET', match: /\/settings\/cover-page\/names\/[^/]+\/revisions/, json: [] },
      ],
      filled('#custom-templateHtml'),
      'cover-page-loaded',
    );
    expect(true).toBe(true);
  });
});
